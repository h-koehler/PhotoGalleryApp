package edu.vt.cs5254.fancygallery

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.Visibility
import coil.disk.DiskCache
import coil.imageLoader
import edu.vt.cs5254.fancygallery.api.FlickrApi
import edu.vt.cs5254.fancygallery.databinding.FragmentGalleryBinding
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create

private const val TAG = "GalleryFragment"

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "FragmentGalleryBinding is null !!!"}

    private val vm: GalleryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_gallery, menu)

//                menu.findItem(R.id.reload_menu).isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.reload_menu -> {
                        context?.imageLoader?.memoryCache?.clear()
                        vm.reloadGalleryItems()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.photoGrid.layoutManager = GridLayoutManager(context, 3)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.galleryItems.collect { galleryItems ->
                    binding.photoGrid.adapter = GalleryListAdapter(galleryItems) { photoPageUri ->
                        // easy way
                        // val intent = Intent(Intent.ACTION_VIEW, photoPageUri)
                        // startActivity(intent)

                        // harder way
                        findNavController()
                            .navigate(GalleryFragmentDirections.showPhoto(photoPageUri))
                    }
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}