package co.edu.unal.myapplication

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.edu.unal.myapplication.data.DatosGovService
import co.edu.unal.myapplication.data.PuntoRecoleccion
import co.edu.unal.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder

private const val TODOS = "Todos"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PantallaPosconsumo()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPosconsumo() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var municipios by remember { mutableStateOf(listOf(TODOS)) }
    var tipos by remember { mutableStateOf(listOf(TODOS)) }
    var municipio by remember { mutableStateOf(TODOS) }
    var tipo by remember { mutableStateOf(TODOS) }
    var empresa by remember { mutableStateOf("") }
    var limite by remember { mutableFloatStateOf(50f) }

    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var urlConsultada by remember { mutableStateOf<String?>(null) }
    var resultados by remember { mutableStateOf<List<PuntoRecoleccion>?>(null) }

    fun consultar() {
        val url = DatosGovService.construirUrl(
            municipio = municipio.takeIf { it != TODOS },
            tipoResiduo = tipo.takeIf { it != TODOS },
            textoEmpresa = empresa,
            limite = limite.toInt()
        )
        urlConsultada = url
        scope.launch {
            cargando = true
            error = null
            try {
                resultados = DatosGovService.buscarPuntos(url)
            } catch (e: Exception) {
                error = "No se pudo consultar el servicio: ${e.message}"
                resultados = null
            } finally {
                cargando = false
            }
        }
    }

    // Al iniciar se consultan al servicio los valores posibles de los filtros.
    LaunchedEffect(Unit) {
        try {
            municipios = listOf(TODOS) + DatosGovService.obtenerMunicipios()
            tipos = listOf(TODOS) + DatosGovService.obtenerTiposDeResiduo()
        } catch (e: Exception) {
            error = "No se pudieron cargar los filtros: ${e.message}"
        }
        consultar()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Puntos Posconsumo", fontWeight = FontWeight.Bold)
                        Text(
                            "Norte de Santander · datos.gov.co",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Parámetros de la consulta",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                Selector("Municipio", municipios, municipio) { municipio = it }
            }
            item {
                Selector("Tipo de residuo", tipos, tipo) { tipo = it }
            }
            item {
                OutlinedTextField(
                    value = empresa,
                    onValueChange = { empresa = it },
                    label = { Text("Empresa / razón social contiene…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column {
                    Text("Máximo de resultados (\$limit): ${limite.toInt()}")
                    Slider(
                        value = limite,
                        onValueChange = { limite = it },
                        valueRange = 5f..200f,
                        steps = 38
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { consultar() },
                        enabled = !cargando,
                        modifier = Modifier.weight(1f)
                    ) { Text("Consultar servicio") }
                    OutlinedButton(onClick = {
                        municipio = TODOS; tipo = TODOS; empresa = ""; limite = 50f
                    }) { Text("Limpiar") }
                }
            }

            urlConsultada?.let { url ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Petición GET enviada:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                URLDecoder.decode(url, "UTF-8"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            when {
                cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> item {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                resultados != null -> {
                    val lista = resultados!!
                    item {
                        Text(
                            "Respuesta: ${lista.size} punto(s) de recolección",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (lista.isEmpty()) {
                        item { Text("No hay puntos que cumplan con los filtros.") }
                    }
                    items(lista) { punto ->
                        TarjetaPunto(punto) {
                            val q = Uri.encode("${punto.direccion}, ${punto.municipio}, ${punto.departamento}, Colombia")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No hay una aplicación de mapas instalada",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.size(8.dp))
                Text(
                    "Fuente: ${DatosGovService.DOCUMENTACION}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Selector(
    etiqueta: String,
    opciones: List<String>,
    seleccionado: String,
    onSeleccion: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = seleccionado,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TarjetaPunto(punto: PuntoRecoleccion, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(punto.razonSocial, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("📍 ${punto.direccion}")
            Text(
                "${punto.municipio}, ${punto.departamento} (DANE ${punto.codigoDane})",
                style = MaterialTheme.typography.bodySmall
            )
            Text("♻️ ${punto.tipoDeResiduos}", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onClick, label = { Text(punto.programa) })
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tocar para ver en el mapa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
