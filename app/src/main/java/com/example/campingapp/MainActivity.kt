package com.example.campingapp

import android.Manifest
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.campingapp.database.*
import com.example.campingapp.ui.theme.CampingAppTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

// Configuración de Retrofit
val retrofit = retrofit2.Retrofit.Builder()
    .baseUrl("https://dadesobertes.gva.es/")
    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
    .build()

val campingApiService = retrofit.create(CampingApiService::class.java)

/* --- CÓDIGO DE LA SESIÓN 2 ---
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
*/

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CampingAppTheme {
                val context = LocalContext.current
                val db = remember { CampingDatabase.getDatabase(context) }
                val viewModel: FavoriteViewModel = viewModel(
                    factory = FavoriteViewModelFactory(db.favoriteDao())
                )
                val favorites by viewModel.allFavorites.collectAsState()

                var isLoading by remember { mutableStateOf(true) }
                var usingRealLocations by remember { mutableStateOf(false) }
                var campings by remember { mutableStateOf<List<Camping>>(emptyList()) }

                // --- LÓGICA DE CARGA PRO (INTERNET + GEOCODER REAL) ---
                LaunchedEffect(Unit) {
                    try {
                        val response = withContext(Dispatchers.IO) { campingApiService.getCampings() }
                        val networkCampings = response.result.records.map { net ->
                            Camping(
                                id = net.id,
                                nombre = net.nombre,
                                municipio = net.municipio,
                                provincia = net.provincia,
                                categoria = net.categoria ?: "",
                                plazas = net.plazas,
                                direccion = net.direccion,
                                web = net.web ?: "",
                                email = net.email ?: ""
                            )
                        }

                        // --- PROCESO DE GEOCODIFICACIÓN REAL ---
                        withContext(Dispatchers.IO) {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            networkCampings.forEach { camping ->
                                try {
                                    val addresses = geocoder.getFromLocationName(
                                        "${camping.nombre}, ${camping.municipio}, España", 1
                                    )
                                    if (!addresses.isNullOrEmpty()) {
                                        camping.latitude = addresses[0].latitude
                                        camping.longitude = addresses[0].longitude
                                        usingRealLocations = true
                                    } else {
                                        // Backup aproximado si no encuentra dirección específica
                                        camping.latitude = 39.4699 + (Math.random() * 0.1)
                                        camping.longitude = -0.3774 + (Math.random() * 0.1)
                                    }
                                    kotlinx.coroutines.delay(10)
                                } catch (e: Exception) {
                                    camping.latitude = 39.4699 + (Math.random() * 0.1)
                                    camping.longitude = -0.3774 + (Math.random() * 0.1)
                                }
                            }
                        }
                        campings = networkCampings
                    } catch (e: Exception) {
                        Log.e("CAMPING_APP", "Error: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }

                if (isLoading) {
                    SplashScreen()
                } else {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            CampingListScreen(campings, favorites, viewModel, navController, usingRealLocations)
                        }
                        composable(
                            "detail/{campingId}",
                            arguments = listOf(navArgument("campingId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val campingId = backStackEntry.arguments?.getInt("campingId") ?: -1
                            val camping = campings.find { it.id == campingId }
                            camping?.let { CampingDetailScreen(it, navController, viewModel, favorites) }
                        }
                        composable("favorites") {
                            FavoritesScreen(campings, favorites, viewModel, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocationOn, null, Modifier.size(100.dp), MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sincronizando con GVA...", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
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
    navController: NavController,
    usingRealLocations: Boolean
) {
    val context = LocalContext.current
    var sortOption by rememberSaveable { mutableStateOf(SortOption.NAME_ASC) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProvince by rememberSaveable { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    var userLocation by remember { mutableStateOf<Location?>(null) }
    var isCalculatingDistances by remember { mutableStateOf(false) }
    var distancesReady by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) userLocation = location
                    else Toast.makeText(context, "Abre Maps en el emulador primero", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) { }
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            isCalculatingDistances = true
            withContext(Dispatchers.IO) {
                campings.forEach { camping ->
                    if (camping.latitude != null && camping.longitude != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(loc.latitude, loc.longitude, camping.latitude!!, camping.longitude!!, results)
                        camping.distanceToUser = results[0]
                    }
                }
                withContext(Dispatchers.Main) { distancesReady = !distancesReady; isCalculatingDistances = false }
            }
        }
    }

    val categories = remember(campings) {
        val allCats = campings.map { it.categoria.ifBlank { "Sin categoría" } }.distinct()
        val estrellas = listOf("CINCO ESTRELLAS", "CUATRO ESTRELLAS", "TRES ESTRELLAS", "DOS ESTRELLAS", "UNA ESTRELLA")
        val estrellasList = estrellas.filter { cat -> allCats.any { it.contains(cat) } }
        val resto = allCats.filter { cat -> cat !in estrellasList && cat != "PERNOCTA" && cat != "Sin categoría" }.sorted()
        estrellasList + resto
    }

    val provinces = remember(campings) { campings.map { it.provincia }.distinct().sorted() }

    val filteredCampings = campings.filter { camping ->
        val matchesSearch = searchText.isBlank() || camping.nombre.contains(searchText, true) || camping.municipio.contains(searchText, true) || camping.provincia.contains(searchText, true)
        val matchesCategory = selectedCategory == null || camping.categoria == selectedCategory
        val matchesProvince = selectedProvince == null || camping.provincia == selectedProvince
        matchesSearch && matchesCategory && matchesProvince
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
                    Text(
                        text = if (usingRealLocations) "Ubicaciones reales" else "Ubicaciones aprox.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (usingRealLocations) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    if (isCalculatingDistances) CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    IconButton(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Icon(Icons.Default.LocationOn, "GPS") }
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menú") }

                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Nombre (A-Z)") }, onClick = { sortOption = SortOption.NAME_ASC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Nombre (Z-A)") }, onClick = { sortOption = SortOption.NAME_DESC; menuExpanded = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Plazas (Más plazas ↓)") }, onClick = { sortOption = SortOption.PLAZAS_DESC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Plazas (Menos plazas ↑)") }, onClick = { sortOption = SortOption.PLAZAS_ASC; menuExpanded = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Categoría (Mejor calidad ★)") }, onClick = { sortOption = SortOption.CATEGORIA_DESC; menuExpanded = false })
                        DropdownMenuItem(text = { Text("Categoría (Menos estrellas)") }, onClick = { sortOption = SortOption.CATEGORIA_ASC; menuExpanded = false })
                        HorizontalDivider()
                        if (userLocation != null || distancesReady) {
                            DropdownMenuItem(text = { Text("Distancia (Más cercanos 📍)") }, onClick = { sortOption = SortOption.DISTANCE_ASC; menuExpanded = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("favorites") }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Favorite, "Favoritos", tint = Color.Red)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchText, onValueChange = { searchText = it },
                label = { Text("Buscar camping, municipio o provincia") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Text(text = "Categoría", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            LazyRow(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("Todas") }) }
                items(categories) { cat -> FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = if (selectedCategory == cat) null else cat }, label = { Text(cat.convertStarsToSymbols()) }) }
            }
            Text(text = "Provincia", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 16.dp))
            LazyRow(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedProvince == null, onClick = { selectedProvince = null }, label = { Text("Todas") }) }
                items(provinces) { prov -> FilterChip(selected = selectedProvince == prov, onClick = { selectedProvince = if (selectedProvince == prov) null else prov }, label = { Text(prov) }) }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sortedCampings) { camping ->
                    CampingItem(camping, favorites.any { it.id == camping.id }, { navController.navigate("detail/${camping.id}") }, {
                        if (favorites.any { it.id == camping.id }) viewModel.removeFavorite(camping.id) else viewModel.addFavorite(camping.id)
                    })
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
        else -> 0
    }
}

@Composable
fun CampingItem(camping: Camping, isFavorite: Boolean, onCardClick: () -> Unit, onFavoriteToggle: () -> Unit) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)
    Card(modifier = Modifier.fillMaxWidth().clickable { onCardClick() }, elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).padding(end = 40.dp)) {
                Text(text = camping.nombre, style = MaterialTheme.typography.titleMedium, color = textColor)
                Text(text = "${camping.municipio} (${camping.provincia})", style = MaterialTheme.typography.bodyMedium, color = textColor)
                Text(text = camping.categoria.convertStarsToSymbols(), style = MaterialTheme.typography.bodySmall, color = textColor)
                Text(text = "Plazas: ${camping.plazas}", style = MaterialTheme.typography.bodyMedium, color = textColor)
                if (camping.distanceToUser != null) {
                    val km = camping.distanceToUser!! / 1000
                    Text(text = "📍 A ${String.format("%.1f", km)} km", style = MaterialTheme.typography.labelSmall, color = textColor)
                }
            }
            IconButton(onClick = onFavoriteToggle, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFavorite) Color.Red else Color.Black)
            }
        }
    }
}

fun getStarColors(categoria: String): Pair<Color, Color> {
    return when {
        categoria.contains("CINCO ESTRELLAS") -> Pair(Color(0xFF08E710), Color.White)
        categoria.contains("CUATRO ESTRELLAS") -> Pair(Color(0xFFABCB36), Color.White)
        categoria.contains("TRES ESTRELLAS") -> Pair(Color(0xFFF3D038), Color.White)
        categoria.contains("DOS ESTRELLAS") -> Pair(Color(0xFFF55D2D), Color.White)
        // ROJO CORAL CLARITO PARA UNA ESTRELLA
        categoria.contains("UNA ESTRELLA") -> Pair(Color(0xFFFF8A80), Color.White)
        else -> Pair(Color(0xFF757575), Color.White)
    }
}

fun String.convertStarsToSymbols(): String = this.replace("CINCO ESTRELLAS", "★★★★★").replace("CUATRO ESTRELLAS", "★★★★").replace("TRES ESTRELLAS", "★★★").replace("DOS ESTRELLAS", "★★").replace("UNA ESTRELLA", "★")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampingDetailScreen(camping: Camping, navController: NavController, viewModel: FavoriteViewModel, favorites: List<FavoriteEntity>) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)
    val context = LocalContext.current
    val isFavorite = favorites.any { it.id == camping.id }
    Scaffold(topBar = { TopAppBar(title = { Text(camping.nombre) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }, actions = {
        IconButton(onClick = { if (isFavorite) viewModel.removeFavorite(camping.id) else viewModel.addFavorite(camping.id) }) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface)
        }
    }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(camping.nombre, style = MaterialTheme.typography.headlineSmall, color = textColor)
                    Text(camping.categoria.convertStarsToSymbols(), style = MaterialTheme.typography.bodyLarge, color = textColor)
                }
            }
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetalleItem("📍 Municipio", camping.municipio)
                    DetalleItem("🏛️ Provincia", camping.provincia)
                    DetalleItem("🛣️ Dirección", camping.direccion)
                    DetalleItem("👥 Plazas", camping.plazas.toString())
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode("${camping.nombre}, ${camping.municipio}")}"))) }, Modifier.padding(top = 16.dp)) { Text("Ver en Google Maps") }
                    Button(
                        onClick = {
                            camping.web.takeIf { it.isNotBlank() }?.let { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = camping.web.isNotBlank()
                    ) {
                        Text("Visitar web")
                    }

                }
            }
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
fun FavoritesScreen(campings: List<Camping>, favorites: List<FavoriteEntity>, viewModel: FavoriteViewModel, navController: NavController) {
    val favList = campings.filter { c -> favorites.any { it.id == c.id } }
    Scaffold(topBar = { TopAppBar(title = { Text("Mis Favoritos") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }) { padding ->
        if (favList.isEmpty()) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("No tienes favoritos.") }
        else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(favList) { c -> CampingItem(c, true, { navController.navigate("detail/${c.id}") }, { viewModel.removeFavorite(c.id) }) }
        }
    }
}