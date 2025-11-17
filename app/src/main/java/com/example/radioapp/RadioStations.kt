package com.example.radioapp

object RadioStations {
    val stations = listOf(
        RadioStation(
            id = 1,
            name = "PowerTürk",
            url = "http://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio",
            description = "Top Türkçe"
        ),
        RadioStation(
            id = 2,
            name = "Kral Pop",
            url = "http://46.20.3.204/listen.pls",
            description = "Pop Müzik"
        ),
        RadioStation(
            id = 3,
            name = "Radyo D",
            url = "http://37.247.98.8/stream/166/",
            description = "Türkçe Pop"
        ),
        RadioStation(
            id = 4,
            name = "Power FM",
            url = "http://listen.powerapp.com.tr/powerfm/mpeg/icecast.audio",
            description = "Türkçe Slow"
        ),
        RadioStation(
            id = 5,
            name = "Number One FM",
            url = "http://n10101m.mediatriple.net/numberone",
            description = "Pop/Dance"
        ),
        RadioStation(
            id = 6,
            name = "Best FM",
            url = "http://46.20.3.229/",
            description = "Pop/Rock"
        ),
        RadioStation(
            id = 7,
            name = "Radyo Pause",
            url = "http://radyopause.listenpowerapp.com/radyopause/mpeg/icecast.audio",
            description = "Chill/Relax"
        )
    )
}