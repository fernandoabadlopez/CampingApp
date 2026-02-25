package com.example.campingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.campingapp.ui.theme.CampingAppTheme
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si aquí crashea, la app se cerrará: por eso es clave que el parseo sea correcto
        val campings = getCampings()

        setContent {
            CampingAppTheme {
                CampingsScreen(campings = campings)
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
fun CampingsScreen(campings: List<Camping>) {
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var expanded by remember { mutableStateOf(false) }

    val sortedCampings = remember(campings, sortOption) {
        when (sortOption) {
            SortOption.NAME_ASC -> campings.sortedBy { it.nombre.lowercase() }
            SortOption.NAME_DESC -> campings.sortedByDescending { it.nombre.lowercase() }
            SortOption.PLAZAS_ASC -> campings.sortedBy { it.plazas }
            SortOption.PLAZAS_DESC -> campings.sortedByDescending { it.plazas }
            SortOption.CATEGORIA_ASC -> campings.sortedBy { getCategoryValue(it.categoria) }
            SortOption.CATEGORIA_DESC -> campings.sortedByDescending { getCategoryValue(it.categoria) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Campings CV") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Controles de ordenamiento
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Ordenar por:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Menu para seleccionar ordenamiento
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(getSortOptionLabel(sortOption))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir")
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nombre (A-Z)") },
                                    onClick = {
                                        sortOption = SortOption.NAME_ASC
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nombre (Z-A)") },
                                    onClick = {
                                        sortOption = SortOption.NAME_DESC
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Plazas (↑)") },
                                    onClick = {
                                        sortOption = SortOption.PLAZAS_ASC
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Plazas (↓)") },
                                    onClick = {
                                        sortOption = SortOption.PLAZAS_DESC
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Categoría (↑)") },
                                    onClick = {
                                        sortOption = SortOption.CATEGORIA_ASC
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Categoría (↓)") },
                                    onClick = {
                                        sortOption = SortOption.CATEGORIA_DESC
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Lista de campings
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedCampings) { camping ->
                    CampingItem(camping = camping)
                }
            }
        }
    }
}

fun getSortOptionLabel(option: SortOption): String {
    return when (option) {
        SortOption.NAME_ASC -> "Nombre (A-Z)"
        SortOption.NAME_DESC -> "Nombre (Z-A)"
        SortOption.PLAZAS_ASC -> "Plazas (↑)"
        SortOption.PLAZAS_DESC -> "Plazas (↓)"
        SortOption.CATEGORIA_ASC -> "Categoría (↑)"
        SortOption.CATEGORIA_DESC -> "Categoría (↓)"
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
fun CampingItem(camping: Camping) {
    val (backgroundColor, textColor) = getStarColors(camping.categoria)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
