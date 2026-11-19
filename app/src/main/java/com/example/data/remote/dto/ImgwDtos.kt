package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw JSON entry returned by https://danepubliczne.imgw.pl/api/data/synop.
 * All numeric telemetry fields arrive as strings or null from IMGW-PIB.
 */
@Serializable
data class ImgwSynopDto(
    @SerialName("id_stacji") val stationId: String = "",
    @SerialName("stacja") val stationName: String = "",
    @SerialName("data_pomiaru") val measurementDate: String? = null,
    @SerialName("godzina_pomiaru") val measurementHour: String? = null,
    @SerialName("temperatura") val temperature: String? = null,
    @SerialName("predkosc_wiatru") val windSpeedMs: String? = null,
    @SerialName("kierunek_wiatru") val windDirectionDeg: String? = null,
    @SerialName("wilgotnosc_wzgledna") val relativeHumidity: String? = null,
    @SerialName("suma_opadu") val precipitationSum: String? = null,
    @SerialName("cisnienie") val pressureHpa: String? = null,
)

/**
 * Raw JSON entry returned by https://danepubliczne.imgw.pl/api/data/warningsmeteo.
 */
@Serializable
data class ImgwWarningDto(
    @SerialName("id") val id: String = "",
    @SerialName("nazwa_zdarzenia") val eventName: String = "",
    @SerialName("stopien") val level: String? = null,
    @SerialName("prawdopodobienstwo") val probability: String? = null,
    @SerialName("obowiazuje_do") val validTo: String? = null,
    @SerialName("obowiazuje_od") val validFrom: String? = null,
    @SerialName("opublikowano") val publishedAt: String? = null,
    @SerialName("tresc") val content: String? = null,
    @SerialName("komentarz") val comment: String? = null,
    @SerialName("biuro") val office: String? = null,
    @SerialName("teryt") val teryt: List<String> = emptyList(),
)
