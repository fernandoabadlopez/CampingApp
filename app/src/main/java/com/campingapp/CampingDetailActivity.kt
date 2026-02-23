package com.campingapp

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.campingapp.adapter.CommentAdapter
import com.campingapp.databinding.ActivityCampingDetailBinding
import com.campingapp.model.Camping
import kotlinx.coroutines.launch

class CampingDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAMPING = "extra_camping"
    }

    private lateinit var binding: ActivityCampingDetailBinding
    private lateinit var commentAdapter: CommentAdapter

    private val camping: Camping by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CAMPING, Camping::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CAMPING)!!
        }
    }

    private val viewModel: DetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetailViewModel(application, camping.id) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = camping.name

        populateCampingDetails()
        setupCommentsSection()
        observeComments()
    }

    private fun populateCampingDetails() {
        binding.textName.text = camping.name
        binding.textCategory.text = camping.category
        binding.textMunicipality.text = camping.municipality
        binding.textProvince.text = camping.province
        binding.textAddress.text = camping.address
        binding.textPostalCode.text = camping.postalCode
        binding.textPhone.text = camping.phone.ifEmpty { getString(R.string.not_available) }
        binding.textEmail.text = camping.email.ifEmpty { getString(R.string.not_available) }
        binding.textWebsite.text = camping.website.ifEmpty { getString(R.string.not_available) }
        binding.textCapacity.text = if (camping.capacity.isNotEmpty()) {
            getString(R.string.capacity_places, camping.capacity)
        } else {
            getString(R.string.not_available)
        }
    }

    private fun setupCommentsSection() {
        commentAdapter = CommentAdapter { comment ->
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_comment)
                .setMessage(R.string.delete_comment_confirm)
                .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteComment(comment) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        binding.recyclerComments.adapter = commentAdapter

        binding.buttonAddComment.setOnClickListener {
            showAddCommentDialog()
        }
    }

    private fun showAddCommentDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.enter_comment)
            maxLines = 4
            setPadding(48, 16, 48, 16)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_comment)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                viewModel.addComment(input.text.toString())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeComments() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.comments.collect { comments ->
                    commentAdapter.submitList(comments)
                    binding.textNoComments.visibility =
                        if (comments.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
