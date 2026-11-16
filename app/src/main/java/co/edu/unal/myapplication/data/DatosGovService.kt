package co.edu.unal.myapplication.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente del servicio web (API SODA / Socrata) de datos.gov.co para el conjunto de datos
 * "Puntos de recolección de Aparatos eléctricos y electrónicos en el departamento
 * Norte de Santander" (programas posconsumo).
 *
 * Documentación: https://www.datos.gov.co/d/c2fr-ezse
 * Endpoint JSON:  https://www.datos.gov.co/resource/c2fr-ezse.json
 *
 * El JSON se procesa únicamente con las clases org.json (JSONArray / JSONObject)
 * que trae el SDK de Android.
 */
object DatosGovService {

    const val ENDPOINT = "https://www.datos.gov.co/resource/c2fr-ezse.json"
    const val DOCUMENTACION = "https://www.datos.gov.co/d/c2fr-ezse"

    /** Lista de municipios distintos (consulta con $group al servicio). */
    suspend fun obtenerMunicipios(): List<String> =
        valoresDistintos("municipio")

    /** Lista de tipos de residuo distintos (consulta con $group al servicio). */
    suspend fun obtenerTiposDeResiduo(): List<String> =
        valoresDistintos("tipo_de_residuos")

    /**
     * Construye la URL de consulta con los parámetros elegidos por el usuario.
     * Se expone para mostrarla en la interfaz.
     */
    fun construirUrl(
        municipio: String?,
        tipoResiduo: String?,
        textoEmpresa: String,
        limite: Int
    ): String {
        val condiciones = mutableListOf<String>()
        if (!municipio.isNullOrBlank()) {
            condiciones += "municipio='${escapar(municipio)}'"
        }
        if (!tipoResiduo.isNullOrBlank()) {
            condiciones += "tipo_de_residuos='${escapar(tipoResiduo)}'"
        }
        if (textoEmpresa.isNotBlank()) {
            condiciones += "upper(raz_n_social) like '%${escapar(textoEmpresa.trim().uppercase())}%'"
        }

        val parametros = linkedMapOf(
            "\$limit" to limite.toString(),
            "\$order" to "municipio, raz_n_social"
        )
        if (condiciones.isNotEmpty()) {
            parametros["\$where"] = condiciones.joinToString(" AND ")
        }
        return ENDPOINT + "?" + parametros.entries.joinToString("&") { (k, v) ->
            "${codificar(k)}=${codificar(v)}"
        }
    }

    /** Ejecuta la consulta y convierte la respuesta JSON en objetos de dominio. */
    suspend fun buscarPuntos(url: String): List<PuntoRecoleccion> {
        val json = JSONArray(get(url))
        val resultado = ArrayList<PuntoRecoleccion>(json.length())
        for (i in 0 until json.length()) {
            val o: JSONObject = json.getJSONObject(i)
            resultado += PuntoRecoleccion(
                razonSocial = o.optString("raz_n_social", "Sin nombre"),
                direccion = o.optString("direccion", "Sin dirección"),
                municipio = o.optString("municipio"),
                departamento = o.optString("departamento"),
                codigoDane = o.optString("codigo_dane"),
                tipoDeResiduos = o.optString("tipo_de_residuos"),
                programa = o.optString("programa_de_recolecci_n")
            )
        }
        return resultado
    }

    private suspend fun valoresDistintos(campo: String): List<String> {
        val url = ENDPOINT + "?" +
                "${codificar("\$select")}=${codificar(campo)}" +
                "&${codificar("\$group")}=${codificar(campo)}" +
                "&${codificar("\$order")}=${codificar(campo)}"
        val json = JSONArray(get(url))
        return (0 until json.length())
            .map { json.getJSONObject(it).optString(campo) }
            .filter { it.isNotBlank() }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 15_000
            conexion.setRequestProperty("Accept", "application/json")
            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                val error = conexion.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("El servicio respondió HTTP $codigo ${error.orEmpty()}")
            }
            conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    private fun escapar(valor: String) = valor.replace("'", "''")

    private fun codificar(valor: String) = URLEncoder.encode(valor, "UTF-8")
}
