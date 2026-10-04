package com.example.plag_out

/** Only a matching, complete server acknowledgement may clear the durable draft. */
internal fun confirmacionGuardada(body: PrediccionConfirmacionResponse?, prediccionId: Int, respuesta: String): Boolean =
    body != null && body.prediccion_id == prediccionId && body.respuesta == respuesta &&
        !body.id.isNullOrBlank() && !body.respondido_en.isNullOrBlank() &&
        (respuesta != "presente" || body.biofix != null)
