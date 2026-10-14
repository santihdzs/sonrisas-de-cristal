package mx.tec.sonrisas.domain

/** reglas de quien ve a que nino (RF-02, RF-07, RF-17) */
object Visibilidad {
    fun ninos(usuario: Usuario?, todos: List<Nino>): List<Nino> = when (usuario?.rol) {
        Rol.EDUCADORA -> todos.filter { it.estanciaId == usuario.estanciaId && it.salaId == usuario.salaId && it.activo }
        Rol.FAMILIAR -> todos.filter { usuario.tutorId != null && usuario.tutorId in it.tutorIds && it.activo }
        Rol.DIRECCION -> todos.filter { it.estanciaId == usuario.estanciaId }
        // la fundacion solo ve datos consolidados, nunca expedientes individuales
        Rol.FUNDACION, null -> emptyList()
    }

    /** salas cuyo contenido (objetos perdidos, material, chat) puede ver el usuario */
    fun salas(usuario: Usuario?, salas: List<Sala>, ninos: List<Nino>): Set<String> = when (usuario?.rol) {
        Rol.EDUCADORA -> setOfNotNull(usuario.salaId)
        Rol.DIRECCION -> salas.filter { it.estanciaId == usuario.estanciaId }.map { it.id }.toSet()
        Rol.FAMILIAR -> ninos(usuario, ninos).map { it.salaId }.toSet()
        Rol.FUNDACION, null -> emptySet()
    }

    /** educadoras de la sala y direccion de su estancia */
    fun esPersonalDe(usuario: Usuario?, sala: Sala?): Boolean = sala != null && when (usuario?.rol) {
        Rol.EDUCADORA -> usuario.salaId == sala.id
        Rol.DIRECCION -> usuario.estanciaId == sala.estanciaId
        else -> false
    }
}
