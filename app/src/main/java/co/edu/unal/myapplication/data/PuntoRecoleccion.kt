package co.edu.unal.myapplication.data

/**
 * Un punto de recolección posconsumo tal como lo devuelve el servicio web
 * del Catálogo de Datos Abiertos de Colombia (dataset c2fr-ezse).
 */
data class PuntoRecoleccion(
    val razonSocial: String,
    val direccion: String,
    val municipio: String,
    val departamento: String,
    val codigoDane: String,
    val tipoDeResiduos: String,
    val programa: String
)
