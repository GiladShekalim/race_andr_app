package com.example.avoided_race_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.avoided_race_app.db.ScoreEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ScoreMapFragment : Fragment() {

    private lateinit var mapView: MapView
    private var allEntries: List<ScoreEntry> = emptyList()

    private val DEFAULT_CENTER = GeoPoint(32.0853, 34.7818)
    private val DEFAULT_ZOOM = 10.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_score_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.score_MAP)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(DEFAULT_CENTER)

        loadAndPinAllScores()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun loadAndPinAllScores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val scores = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()
            withContext(Dispatchers.Main) {
                allEntries = scores
                pinAllScores(highlightedEntry = null)
            }
        }
    }

    private fun pinAllScores(highlightedEntry: ScoreEntry?) {
        mapView.overlays.clear()

        val validEntries = allEntries.filter { it.latitude != 0.0 || it.longitude != 0.0 }

        if (validEntries.isEmpty()) {
            mapView.controller.setCenter(DEFAULT_CENTER)
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.invalidate()
            return
        }

        validEntries.forEach { entry ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(entry.latitude, entry.longitude)
            marker.title = "Score: ${entry.score}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            if (entry == highlightedEntry) {
                marker.icon = ContextCompat.getDrawable(
                    requireContext(), org.osmdroid.library.R.drawable.marker_default
                )?.mutate()?.also { it.setTint(0xFFFFD700.toInt()) }
            }

            mapView.overlays.add(marker)
        }

        if (highlightedEntry == null && validEntries.isNotEmpty()) {
            val top = validEntries.first()
            mapView.controller.animateTo(GeoPoint(top.latitude, top.longitude))
            mapView.controller.setZoom(12.0)
        }

        mapView.invalidate()
    }

    fun centerOn(latitude: Double, longitude: Double) {
        val target = GeoPoint(latitude, longitude)
        mapView.controller.animateTo(target)
        mapView.controller.setZoom(14.0)

        val selected = allEntries.find { it.latitude == latitude && it.longitude == longitude }
        pinAllScores(highlightedEntry = selected)
    }
}
