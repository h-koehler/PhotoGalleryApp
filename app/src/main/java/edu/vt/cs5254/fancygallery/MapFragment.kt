package edu.vt.cs5254.fancygallery

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withCreated
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import edu.vt.cs5254.fancygallery.api.GalleryItem
import edu.vt.cs5254.fancygallery.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "FragmentMapBinding is null !!!" }

    private val vm: MapViewModel by viewModels()
    private val activityVM: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName

        binding.mapView.apply {
            minZoomLevel = 1.5
            maxZoomLevel = 15.0
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0
            )
            isVerticalMapRepetitionEnabled = false
            isTilesScaledToDpi = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            setTileSource(TileSourceFactory.MAPNIK)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                activityVM.galleryItems.collect { item ->
                    item.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        .forEach { galleryItem ->
                            val photoDrawable = loadDrawableFromUrl(galleryItem.url)
                            photoDrawable?.let { drawable ->
                                val marker = Marker(binding.mapView).apply {
                                    position = GeoPoint(galleryItem.latitude, galleryItem.longitude)
                                    title = galleryItem.title
                                    icon = drawable
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    relatedObject = galleryItem
                                    setOnMarkerClickListener { marker, mapView ->
                                        mapView.apply {
                                            controller.animateTo(marker.position)
                                            overlays.remove(marker)
                                            overlays.add(marker)
                                        }
                                        if (marker.isInfoWindowShown) {
                                            val item = marker.relatedObject as GalleryItem
                                            findNavController().navigate(
                                                MapFragmentDirections.showPhotoFromMarker(item.photoPageUri)
                                            )
                                        } else {
                                            showInfoWindow()
                                        }
                                        true
                                    }
                                }
                                _binding?.mapView?.overlays?.add(marker)
                            }
                        }
                    _binding?.mapView?.invalidate()
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        with(binding.mapView) {
            vm.saveMapState(zoomLevelDouble, mapCenter)
            onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding.mapView) {
            onResume()
            controller.setZoom(vm.zoomLevel)
            controller.setCenter(vm.mapCenter)
        }
//        Log.w("SHARED VM TEST", "Found ${activityVM.galleryItems.value.size} gallery items")
//        val count = activityVM.galleryItems.value.count {
//            it.latitude != 0.0 && it.longitude != 0.0
//        }
//        Log.w("SHARED VM TEST", "Found $count gallery items with valid geo")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    private suspend fun loadDrawableFromUrl(url: String): Drawable? {
        return context?.let {
            val loader = it.imageLoader
            val request = ImageRequest.Builder(requireContext())
                .data(url)
                .build()
            return try {
                val result = loader.execute(request)
                (result as SuccessResult).drawable
            } catch (ex: Exception) {
                null
            }
        }
    }
}