package com.example.campingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampingsScreen(campings: List<Camping>) {
    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("Campings CV") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(campings) { camping ->
                CampingItem(camping = camping)
            }
        }
    }
}

@Composable
fun CampingItem(camping: Camping) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = camping.nombre, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${camping.municipio} (${camping.provincia})", style = MaterialTheme.typography.bodyMedium)
            if (camping.categoria.isNotBlank()) {
                Text(text = camping.categoria, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Plazas: ${camping.plazas}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}