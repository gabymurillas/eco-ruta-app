package com.ecoruta.dispositivo.domain.model

/**
 * A route name the admin lets this unit show instead of its usual route (a `letrero`, like the
 * sign on the windscreen). It only changes what the web displays; the route's stops and trace stay.
 */
data class RouteSign(
    val id: String,
    val name: String,
)

/** What the driver may pick for this unit, and what is shown now (null: the usual route). */
data class RouteSignOptions(
    val current: RouteSign?,
    val options: List<RouteSign>,
)
