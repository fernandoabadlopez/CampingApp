package com.example.campingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campingapp.ui.theme.CampingAppTheme
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val campings = getCampings()

        setContent {
            CampingAppTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "list"
                ) {
                    composable("list") {
                        CampingListScreen(
                            campings = campings,
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
                            CampingDetailScreen(camping = it, navController = navController)
                        }
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
        // Tu archivo actual es camping.json => R.raw.camping
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
    NAME_ASC,
    NAME_DESC,
    PLAZAS_ASC,
    PLAZAS_DESC,
    CATEGORIA_ASC,
    CATEGORIA_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampingListScreen(campings: List<Camping>, navController: NavController) {
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedProvince by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }

    val categories = remember(campings) { campings.map { it.categoria }.distinct().sorted() }
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

    val sortedCampings = remember(filteredCampings, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> filteredCampings.sortedBy { it.nombre.lowercase() }
            SortOption.NAME_DESC -> filteredCampings.sortedByDescending { it.nombre.lowercase() }
            SortOption.PLAZAS_ASC -> filteredCampings.sortedBy { it.plazas }
            SortOption.PLAZAS_DESC -> filteredCampings.sortedByDescending { it.plazas }
            SortOption.CATEGORIA_ASC -> filteredCampings.sortedBy { getCategoryValue(it.categoria) }
            SortOption.CATEGORIA_DESC -> filteredCampings.sortedByDescending { getCategoryValue(it.categoria) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campings CV") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nombre (A-Z)") },
                            onClick = {
                                sortOption = SortOption.NAME_ASC
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nombre (Z-A)") },
                            onClick = {
                                sortOption = SortOption.NAME_DESC
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Plazas (↑)") },
                            onClick = {
                                sortOption = SortOption.PLAZAS_ASC
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Plazas (↓)") },
                            onClick = {
                                sortOption = SortOption.PLAZAS_DESC
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Categoría (↑)") },
                            onClick = {
                                sortOption = SortOption.CATEGORIA_ASC
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Categoría (↓)") },
                            onClick = {
                                sortOption = SortOption.CATEGORIA_DESC
                                menuExpanded = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar camping, municipio o provincia") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true
            )
            // Filtros por categoría
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todas") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.convertStarsToSymbols()) }
                    )
                }
            }
            // Filtros por provincia
            Text(
                text = "Provincia",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedProvince == null,
                        onClick = { selectedProvince = null },
                        label = { Text("Todas") }
                    )
                }
                items(provinces) { prov ->
                    FilterChip(
                        selected = selectedProvince == prov,
                        onClick = { selectedProvince = if (selectedProvince == prov) null else prov },
                        label = { Text(prov) }
                    )
                }
            }
            // Filtros por tipo de alojamiento (si existen)
            if (types.isNotEmpty()) {
                Text(
                    text = "Tipo de alojamiento",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("Todos") }
                        )
                    }
                    items(types) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = { Text(type) }
                        )
                    }
                }
            }
            // Lista de campings limpia
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedCampings) { camping ->
                    CampingItem(
                        camping = camping,
                        onClick = {
                            navController.navigate("detail/${camping.id}")
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
fun CampingItem( camping: Camping,
                 onClick: () -> Unit) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = camping.nombre, style = MaterialTheme.typography.titleMedium, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${camping.municipio} (${camping.provincia})", style = MaterialTheme.typography.bodyMedium, color = textColor)
            if (camping.categoria.isNotBlank()) {
                Text(text = camping.categoria.convertStarsToSymbols(), style = MaterialTheme.typography.bodySmall, color = textColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Plazas: ${camping.plazas}", style = MaterialTheme.typography.bodyMedium, color = textColor)
        }
    }
}

fun getStarColors(categoria: String): Pair<Color, Color> {
    return when {
        categoria.contains("CINCO ESTRELLAS") -> Pair(Color(0xFF08E710), Color(0xFFFFFFFF)) // Verde vibrante con blanco
        categoria.contains("CUATRO ESTRELLAS") -> Pair(Color(0xFFABCB36), Color(0xFFFFFFFF)) // Púrpura-azul con blanco
        categoria.contains("TRES ESTRELLAS") -> Pair(Color(0xFFF3D038), Color(0xFFFFFFFF)) // Púrpura-azul más oscuro con blanco
        categoria.contains("DOS ESTRELLAS") -> Pair(Color(0xFFF55D2D), Color(0xFFFFFFFF)) // Naranja brillante con blanco
        categoria.contains("UNA ESTRELLA") -> Pair(Color(0xFFFF0000), Color(0xFFFFFFFF)) // Rojo vibrante con blanco
        categoria.contains("PERNOCTA") -> Pair(Color(0xFF9E9E9E), Color(0xFFFFFFFF)) // Gris con blanco
        else -> Pair(Color(0xFF757575), Color(0xFFFFFFFF)) // Gris más oscuro con blanco
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
fun CampingDetailScreen(camping: Camping, navController: NavController) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // Opción: Abrir en Maps
                        DropdownMenuItem(
                            text = { Text("📍 Ver en Maps") },
                            onClick = {
                                val geoUri = Uri.parse("geo:0,0?q=${Uri.encode("${camping.nombre}, ${camping.municipio}, ${camping.provincia}")}")
                                val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                context.startActivity(intent)
                                menuExpanded = false
                            }
                        )

                        // Opción: Abrir sitio web (solo si existe)
                        if (camping.web.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("🌐 Abrir sitio web") },
                                onClick = {
                                    val webUrl = if (camping.web.startsWith("http")) {
                                        camping.web
                                    } else {
                                        "http://${camping.web}"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                    context.startActivity(intent)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Card con información de categoría
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        camping.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        camping.categoria.convertStarsToSymbols(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            }

            // Card con información general
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Información General",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetalleItem("📍 Municipio", camping.municipio)
                    DetalleItem("🏛️ Provincia", camping.provincia)
                    DetalleItem("🛣️ Dirección", camping.direccion)
                    DetalleItem("👥 Plazas", camping.plazas.toString())
                }
            }

            // Card con contacto si existe
            if (camping.web.isNotEmpty() || camping.email.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Contacto",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (camping.web.isNotEmpty()) {
                            DetalleItem("🌐 Web", camping.web)
                        }
                        if (camping.email.isNotEmpty()) {
                            DetalleItem("📧 Email", camping.email)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetalleItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
