package com.example.campingapp

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // IMPORTANTE: Importamos rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campingapp.database.CampingDatabase
import com.example.campingapp.database.FavoriteViewModel
import com.example.campingapp.database.FavoriteViewModelFactory
import com.example.campingapp.database.FavoriteEntity
import com.example.campingapp.ui.theme.CampingAppTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val campings = getCampings()

        setContent {
            CampingAppTheme {
                val context = LocalContext.current

                val db = remember { CampingDatabase.getDatabase(context) }
                val viewModel: FavoriteViewModel = viewModel(
                    factory = FavoriteViewModelFactory(db.favoriteDao())
                )
                val favorites by viewModel.allFavorites.collectAsState()

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        CampingListScreen(
                            campings = campings,
                            favorites = favorites,
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                    composable(
                        "detail/{campingId}",
                        arguments = listOf(navArgument("campingId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val campingId = backStackEntry.arguments?.getInt("campingId") ?: -1
                        val camping = campings.find { it.id == campingId }
                        camping?.let {
                            CampingDetailScreen(camping = it, navController = navController, viewModel = viewModel, favorites = favorites)
                        }
                    }
                    composable("favorites") {
                        FavoritesScreen(campings = campings, favorites = favorites, viewModel = viewModel, navController = navController)
                    }
                }
            }
        }
    }

    private fun readJsonFromRaw(resId: Int): String {
        val inputStream = resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }

    private fun getCampings(): List<Camping> {
        val jsonString = readJsonFromRaw(R.raw.campings)
        val root = JSONObject(jsonString)
        val result = root.getJSONObject("result")
        val records = result.getJSONArray("records")
        val list = ArrayList<Camping>(records.length())

        for (i in 0 until records.length()) {
            val obj = records.getJSONObject(i)
            val camping = Camping(
                id = obj.optInt("_id", -1),
                nombre = obj.optString("Nombre", "").trim(),
                municipio = obj.optString("Municipio", "").trim(),
                provincia = obj.optString("Provincia", "").trim(),
                categoria = obj.optString("Categoria", "").trim(),
                plazas = obj.optInt("Plazas", 0),
                direccion = obj.optString("Direccion", "").trim(),
                web = obj.optString("Web", "").trim(),
                email = obj.optString("Email", "").trim()
            )
            if (camping.nombre.isNotEmpty()) list.add(camping)
        }
        return list
    }
}

enum class SortOption {
    NAME_ASC, NAME_DESC, PLAZAS_ASC, PLAZAS_DESC, CATEGORIA_ASC, CATEGORIA_DESC, DISTANCE_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampingListScreen(
    campings: List<Camping>,
    favorites: List<FavoriteEntity>,
    viewModel: FavoriteViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    // --- MAGIA APLICADA AQUÍ: Usamos rememberSaveable para todo ---
    var sortOption by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProvince by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }

    // El menú no hace falta guardarlo al navegar
    var menuExpanded by remember { mutableStateOf(false) }

    // --- VARIABLES DE LOCALIZACIÓN ---
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var isCalculatingDistances by remember { mutableStateOf(false) }
    var distancesReady by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    userLocation = location
                }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            isCalculatingDistances = true

            withContext(Dispatchers.IO) {
                campings.forEach { camping ->
                    if (camping.latitude == null) {
                        camping.latitude = 39.4699 + (Math.random() * 1.5 - 0.75)
                        camping.longitude = -0.3774 + (Math.random() * 1.5 - 0.75)
                    }

                    if (camping.latitude != null && camping.longitude != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            loc.latitude, loc.longitude,
                            camping.latitude!!, camping.longitude!!,
                            results
                        )
                        camping.distanceToUser = results[0]
                    }
                }

                withContext(Dispatchers.Main) {
                    distancesReady = !distancesReady
                    isCalculatingDistances = false
                }
            }
        }
    }

    val categories = remember(campings) {
        val allCats = campings.map { it.categoria.ifBlank { "Sin categoría" } }.distinct()
        val estrellas = listOf("CINCO ESTRELLAS", "CUATRO ESTRELLAS", "TRES ESTRELLAS", "DOS ESTRELLAS", "UNA ESTRELLA")
        val pernocta = "PERNOCTA"
        val sinCategoria = "Sin categoría"
        val estrellasList = estrellas.filter { cat -> allCats.any { it.contains(cat) } }
        val pernoctaList = if (allCats.any { it == pernocta }) listOf(pernocta) else emptyList()
        val sinCategoriaList = if (allCats.any { it == sinCategoria }) listOf(sinCategoria) else emptyList()
        val resto = allCats.filter { cat -> cat !in estrellasList && cat != pernocta && cat != sinCategoria }.sorted()
        estrellasList + sinCategoriaList + resto + pernoctaList
    }

    val provinces = remember(campings) { campings.map { it.provincia }.distinct().sorted() }
    val types = remember(campings) {
        campings.map {
            val parts = it.categoria.split("-")
            if (parts.size > 1) parts[1].trim() else null
        }.filterNotNull().distinct().sorted()
    }

    val filteredCampings = campings.filter { camping ->
        val matchesSearch = searchText.isBlank() ||
                camping.nombre.contains(searchText, ignoreCase = true) ||
                camping.municipio.contains(searchText, ignoreCase = true) ||
                camping.provincia.contains(searchText, ignoreCase = true)
        val matchesCategory = selectedCategory == null || camping.categoria == selectedCategory
        val matchesProvince = selectedProvince == null || camping.provincia == selectedProvince
        val matchesType = selectedType == null || (camping.categoria.split("-").getOrNull(1)?.trim() == selectedType)
        matchesSearch && matchesCategory && matchesProvince && matchesType
    }

    val sortedCampings = remember(filteredCampings, sortOption, distancesReady) {
        when (sortOption) {
            SortOption.NAME_ASC -> filteredCampings.sortedBy { it.nombre.lowercase() }
            SortOption.NAME_DESC -> filteredCampings.sortedByDescending { it.nombre.lowercase() }
            SortOption.PLAZAS_ASC -> filteredCampings.sortedBy { it.plazas }
            SortOption.PLAZAS_DESC -> filteredCampings.sortedByDescending { it.plazas }
            SortOption.CATEGORIA_ASC -> filteredCampings.sortedBy { getCategoryValue(it.categoria) }
            SortOption.CATEGORIA_DESC -> filteredCampings.sortedByDescending { getCategoryValue(it.categoria) }
            SortOption.DISTANCE_ASC -> filteredCampings.sortedBy { it.distanceToUser ?: Float.MAX_VALUE }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campings CV") },
                actions = {
                    if (isCalculatingDistances) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Activar GPS")
                        }
                    }

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Nombre (A-Z)") }, onClick = { sortOption = SortOption.NAME_ASC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Nombre (Z-A)") }, onClick = { sortOption = SortOption.NAME_DESC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Plazas (↑)") }, onClick = { sortOption = SortOption.PLAZAS_ASC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Plazas (↓)") }, onClick = { sortOption = SortOption.PLAZAS_DESC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Categoría (↑)") }, onClick = { sortOption = SortOption.CATEGORIA_ASC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Categoría (↓)") }, onClick = { sortOption = SortOption.CATEGORIA_DESC; menuExpanded = false })

                        if (userLocation != null) {
                            DropdownMenuItem(text = { Text("Distancia (Más cercanos)") }, onClick = { sortOption = SortOption.DISTANCE_ASC; menuExpanded = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("favorites") }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Favorite, contentDescription = "Ver Favoritos", tint = Color.Red)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchText, onValueChange = { searchText = it }, label = { Text("Buscar camping, municipio o provincia") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), singleLine = true
            )
            Text(text = "Categoría", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("Todas") }) }
                items(categories) { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = if (selectedCategory == cat) null else cat }, label = { Text(cat.convertStarsToSymbols()) }) }
            }
            Text(text = "Provincia", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedProvince == null, onClick = { selectedProvince = null }, label = { Text("Todas") }) }
                items(provinces) { prov -> FilterChip(selected = selectedProvince == prov, onClick = { selectedProvince = if (selectedProvince == prov) null else prov }, label = { Text(prov) }) }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedCampings) { camping ->
                    val isFav = favorites.any { it.id == camping.id }
                    CampingItem(
                        camping = camping,
                        isFavorite = isFav,
                        onCardClick = { navController.navigate("detail/${camping.id}") },
                        onFavoriteToggle = {
                            if (isFav) viewModel.removeFavorite(camping.id)
                            else viewModel.addFavorite(camping.id)
                        }
                    )
                }
            }
        }
    }
}

fun getCategoryValue(categoria: String): Int {
    return when {
        categoria.contains("CINCO ESTRELLAS") -> 5
        categoria.contains("CUATRO ESTRELLAS") -> 4
        categoria.contains("TRES ESTRELLAS") -> 3
        categoria.contains("DOS ESTRELLAS") -> 2
        categoria.contains("UNA ESTRELLA") -> 1
        categoria.contains("PERNOCTA") -> 0
        else -> -1
    }
}

@Composable
fun CampingItem(
    camping: Camping,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).padding(end = 32.dp)) {
                Text(text = camping.nombre, style = MaterialTheme.typography.titleMedium, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${camping.municipio} (${camping.provincia})", style = MaterialTheme.typography.bodyMedium, color = textColor)
                if (camping.categoria.isNotBlank()) {
                    Text(text = camping.categoria.convertStarsToSymbols(), style = MaterialTheme.typography.bodySmall, color = textColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Plazas: ${camping.plazas}", style = MaterialTheme.typography.bodyMedium, color = textColor)

                if (camping.distanceToUser != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val km = camping.distanceToUser!! / 1000
                    Text(text = "📍 A ${String.format("%.1f", km)} km de ti", style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
            }

            IconButton(onClick = { onFavoriteToggle() }, modifier = Modifier.align(Alignment.TopEnd)) {
                if (isFavorite) {
                    Icon(Icons.Default.Favorite, contentDescription = "Quitar", tint = Color.Red)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.LightGray)
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Añadir", tint = Color.Black)
                    }
                }
            }
        }
    }
}

fun getStarColors(categoria: String): Pair<Color, Color> {
    return when {
        categoria.contains("CINCO ESTRELLAS") -> Pair(Color(0xFF08E710), Color(0xFFFFFFFF))
        categoria.contains("CUATRO ESTRELLAS") -> Pair(Color(0xFFABCB36), Color(0xFFFFFFFF))
        categoria.contains("TRES ESTRELLAS") -> Pair(Color(0xFFF3D038), Color(0xFFFFFFFF))
        categoria.contains("DOS ESTRELLAS") -> Pair(Color(0xFFF55D2D), Color(0xFFFFFFFF))
        categoria.contains("UNA ESTRELLA") -> Pair(Color(0xFFFF0000), Color(0xFFFFFFFF))
        categoria.contains("PERNOCTA") -> Pair(Color(0xFF9E9E9E), Color(0xFFFFFFFF))
        else -> Pair(Color(0xFF757575), Color(0xFFFFFFFF))
    }
}

fun String.convertStarsToSymbols(): String {
    return this
        .replace("CINCO ESTRELLAS", "★★★★★")
        .replace("CUATRO ESTRELLAS", "★★★★")
        .replace("TRES ESTRELLAS", "★★★")
        .replace("DOS ESTRELLAS", "★★")
        .replace("UNA ESTRELLA", "★")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampingDetailScreen(
    camping: Camping,
    navController: NavController,
    viewModel: FavoriteViewModel,
    favorites: List<FavoriteEntity>
) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val isFavorite = favorites.any { it.id == camping.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(camping.nombre) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isFavorite) viewModel.removeFavorite(camping.id) else viewModel.addFavorite(camping.id)
                    }) {
                        if (isFavorite) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorito", tint = Color.Red)
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.LightGray)
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Añadir a favoritos", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("📍 Ver en Maps") },
                            onClick = {
                                val geoUri = Uri.parse("geo:0,0?q=${Uri.encode("${camping.nombre}, ${camping.municipio}, ${camping.provincia}")}")
                                val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                context.startActivity(intent)
                                menuExpanded = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("favorites") }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Favorite, contentDescription = "Ver Favoritos", tint = Color.Red)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(camping.nombre, style = MaterialTheme.typography.headlineSmall, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(camping.categoria.convertStarsToSymbols(), style = MaterialTheme.typography.bodyLarge, color = textColor)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetalleItem("📍 Municipio", camping.municipio)
                    DetalleItem("🏛️ Provincia", camping.provincia)
                    DetalleItem("🛣️ Dirección", camping.direccion)
                    DetalleItem("👥 Plazas", camping.plazas.toString())
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetalleItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    campings: List<Camping>,
    favorites: List<FavoriteEntity>,
    viewModel: FavoriteViewModel,
    navController: NavController
) {
    val favoriteCampings = campings.filter { camping -> favorites.any { it.id == camping.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Favoritos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (favoriteCampings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "Aún no tienes campings favoritos.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favoriteCampings) { camping ->
                    CampingItem(camping = camping, isFavorite = true, onCardClick = { navController.navigate("detail/${camping.id}") }, onFavoriteToggle = { viewModel.removeFavorite(camping.id) })
                }
            }
        }
    }
}