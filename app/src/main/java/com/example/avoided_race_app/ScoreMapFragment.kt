package com.example.avoided_race_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.avoided_race_app.db.ScoreEntry
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ScoreMapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var updateButton: MaterialButton

    private var allEntries: List<ScoreEntry> = emptyList()
    private var selectedEntryId: Int = -1
    private var currentMarker: Marker? = null

    private val DEFAULT_CENTER = GeoPoint(32.0853, 34.7818)
    private val DEFAULT_ZOOM = 10.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_score_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.score_MAP)
        updateButton = view.findViewById(R.id.map_BTN_update)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.controller.setCenter(DEFAULT_CENTER)

        updateButton.setOnClickListener { saveUpdatedLocation() }

        // Load entries for label lookups
        lifecycleScope.launch(Dispatchers.IO) {
            allEntries = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    fun centerOn(latitude: Double, longitude: Double, entryId: Int) {
        selectedEntryId = entryId
        val geoPoint = GeoPoint(latitude, longitude)

        mapView.overlays.clear()

        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = allEntries.find { it.id == entryId }?.score?.let { "Score: $it" } ?: "Score"
        marker.isDraggable = true
        currentMarker = marker
        mapView.overlays.add(marker)

        mapView.controller.animateTo(geoPoint)
        mapView.controller.setZoom(14.0)
        mapView.invalidate()

        updateButton.visibility = View.VISIBLE
    }

    private fun saveUpdatedLocation() {
        val marker = currentMarker ?: return
        val entryId = selectedEntryId.takeIf { it != -1 } ?: return

        val newLat = marker.position.latitude
        val newLng = marker.position.longitude

        lifecycleScope.launch(Dispatchers.IO) {
            (requireActivity().application as GameApplication)
                .database.scoreDao().updateLocation(entryId, newLat, newLng)

            // Refresh local cache so subsequent row taps use the updated coordinates
            allEntries = (requireActivity().application as GameApplication)
                .database.scoreDao().getTop10()

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Location updated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
