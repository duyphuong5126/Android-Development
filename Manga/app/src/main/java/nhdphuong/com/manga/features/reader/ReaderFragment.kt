package nhdphuong.com.manga.features.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import nhdphuong.com.manga.Constants
import nhdphuong.com.manga.Logger
import nhdphuong.com.manga.NotificationHelper
import nhdphuong.com.manga.R
import nhdphuong.com.manga.features.NavigationRedirectActivity
import nhdphuong.com.manga.supports.AnimationHelper
import nhdphuong.com.manga.supports.SpaceItemDecoration
import nhdphuong.com.manga.views.PreLoadingLinearLayoutManager
import nhdphuong.com.manga.views.adapters.BookReaderAdapter
import nhdphuong.com.manga.views.adapters.ReaderNavigationAdapter
import nhdphuong.com.manga.views.addSnapPositionChangedListener
import nhdphuong.com.manga.views.becomeVisibleIf
import nhdphuong.com.manga.views.customs.MyTextView
import nhdphuong.com.manga.views.gone
import nhdphuong.com.manga.views.showStoragePermissionDialog

/*
 * Created by nhdphuong on 5/5/18.
 */
class ReaderFragment : Fragment(), ReaderContract.View, View.OnClickListener {

    private lateinit var presenter: ReaderContract.Presenter
    private lateinit var rotationAnimation: Animation
    private lateinit var bookReaderAdapter: BookReaderAdapter
    private lateinit var navigationAdapter: ReaderNavigationAdapter

    private lateinit var clDownloadedPopup: ConstraintLayout
    private lateinit var clReaderTop: ConstraintLayout
    private lateinit var ibBack: ImageButton
    private lateinit var ibDownload: ImageButton
    private lateinit var ibDownloadPopupClose: ImageButton
    private lateinit var ibRefresh: ImageButton
    private lateinit var ibShare: ImageButton
    private lateinit var layoutReaderBottom: LinearLayout
    private lateinit var mtvBookTitle: MyTextView
    private lateinit var mtvCurrentPage: MyTextView
    private lateinit var mtvDownloadTitle: MyTextView
    private lateinit var rvQuickNavigation: RecyclerView
    private lateinit var rvBookPages: RecyclerView

    private var viewDownloadedData = false

    override fun setPresenter(presenter: ReaderContract.Presenter) {
        this.presenter = presenter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUI(view)

        viewDownloadedData = arguments?.getBoolean(Constants.VIEW_DOWNLOADED_DATA) ?: false
        if (viewDownloadedData) {
            presenter.enableViewDownloadedDataMode()
        }
        ibBack.setOnClickListener(this)
        mtvCurrentPage.setOnClickListener(this)
        ibDownload.setOnClickListener(this)
        ibDownloadPopupClose.setOnClickListener(this)
        ibShare.setOnClickListener(this)

        ibDownload.becomeVisibleIf(!viewDownloadedData, otherWiseVisibility = View.INVISIBLE)
        ibShare.becomeVisibleIf(!viewDownloadedData)
        context?.let { context ->
            rotationAnimation = AnimationHelper.getRotationAnimation(context)
            ibRefresh.setOnClickListener(this)
        }

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
                presenter.endReading()
            }
            return@OnKeyListener false
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.start()
        activity?.onBackPressedDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                presenter.forceBackToGallery()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            val permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                showRequestStoragePermission()
            }
            val result = if (permissionGranted) "granted" else "denied"
            Logger.d(TAG, "Storage permission is $result")
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ibShare -> {
                presenter.generateSharableLink()
            }

            R.id.ibBack -> {
                presenter.forceBackToGallery()
            }

            R.id.mtvCurrentPage -> {
                presenter.backToGallery()
            }

            R.id.ibDownload -> {
                presenter.downloadCurrentPage()
            }

            R.id.ibDownloadPopupClose -> {
                hideDownloadPopup()
            }

            R.id.ibRefresh -> {
                ibRefresh.startAnimation(rotationAnimation)
                presenter.reloadCurrentPage { currentPage: Int ->
                    bookReaderAdapter.resetPage(currentPage)
                    ibRefresh.postDelayed(ibRefresh::clearAnimation, 3000)
                }
            }
        }
    }

    override fun showBookTitle(bookTitle: String) {
        mtvBookTitle.let { mtvBookTitle ->
            mtvBookTitle.text = bookTitle
            AnimationHelper.startTextRunning(mtvBookTitle)
        }
    }

    override fun showBookPages(pageList: List<String>) {
        activity?.let { activity ->
            navigationAdapter = ReaderNavigationAdapter(
                pageList,
                object : ReaderNavigationAdapter.ThumbnailSelectListener {
                    override fun onItemSelected(position: Int) {
                        rvBookPages.scrollToPosition(position)
                        onPageChanged(position)
                    }
                })
            rvQuickNavigation.adapter = navigationAdapter
            rvQuickNavigation.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            rvQuickNavigation.addItemDecoration(
                SpaceItemDecoration(
                    activity,
                    R.dimen.space_medium,
                    showFirstDivider = false,
                    showLastDivider = true
                )
            )

            bookReaderAdapter = BookReaderAdapter(pageList) {
                if (layoutReaderBottom.visibility == View.VISIBLE) {
                    AnimationHelper.startSlideOutTop(activity, clReaderTop) {
                        clReaderTop.visibility = View.GONE
                    }
                    AnimationHelper.startSlideOutBottom(activity, layoutReaderBottom) {
                        layoutReaderBottom.visibility = View.GONE
                    }
                } else {
                    AnimationHelper.startSlideInTop(activity, clReaderTop) {
                        clReaderTop.visibility = View.VISIBLE
                    }
                    AnimationHelper.startSlideInBottom(activity, layoutReaderBottom) {
                        layoutReaderBottom.visibility = View.VISIBLE
                    }
                }
            }
            rvBookPages.adapter = bookReaderAdapter
            context?.let {
                rvBookPages.layoutManager =
                    PreLoadingLinearLayoutManager(
                        it,
                        LinearLayoutManager.HORIZONTAL,
                        false,
                        PRELOAD_SIZE
                    )
            }
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(rvBookPages)
            rvBookPages.addSnapPositionChangedListener(snapHelper, this::onPageChanged)
        }
    }

    override fun showPageIndicator(currentPage: Int, total: Int) {
        mtvCurrentPage.text = String.format(getString(R.string.bottom_reader), currentPage, total)
    }

    override fun showBackToGallery() {
        mtvCurrentPage.text = getString(R.string.back_to_gallery)
    }

    override fun jumpToPage(pageNumber: Int) {
        rvBookPages.scrollToPosition(pageNumber)
    }

    override fun navigateToGallery(lastVisitedPage: Int) {
        presenter.endReading()
        activity?.apply {
            val result = Intent().apply {
                putExtra(Constants.LAST_VISITED_PAGE_RESULT, lastVisitedPage)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun showRequestStoragePermission() {
        activity?.showStoragePermissionDialog(onOk = {
            requestStoragePermission()
        }, onDismiss = {
            Toast.makeText(
                activity,
                getString(R.string.toast_storage_permission_require),
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    override fun showDownloadPopup() {
        clDownloadedPopup.visibility = View.VISIBLE
    }

    override fun hideDownloadPopup() {
        clDownloadedPopup.postDelayed({
            clDownloadedPopup.gone()
        }, 3000)
    }

    override fun updateDownloadPopupTitle(downloadPage: Int) {
        mtvDownloadTitle.text = getString(R.string.download_progress, downloadPage.toString())
    }

    override fun pushNowReadingNotification(readingTitle: String, page: Int, total: Int) {
        activity?.let {
            val notificationIntent = Intent(it, NavigationRedirectActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                it, 0,
                notificationIntent, Intent.FILL_IN_ACTION
            )
            NotificationHelper.sendBigContentNotification(
                getString(R.string.now_reading),
                NotificationCompat.PRIORITY_DEFAULT,
                readingTitle,
                true,
                System.currentTimeMillis().toInt(),
                pendingIntent
            ).let { notificationId ->
                presenter.updateNotificationId(notificationId)
            }
        }
    }

    override fun removeNotification(notificationId: Int) {
        NotificationHelper.cancelNotification(notificationId)
    }

    override fun processSharingCurrentPage(bookId: String, bookTitle: String, url: String) {
        val sharingTitle = getString(R.string.page_sharing_title, bookId, bookTitle)
        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_SUBJECT, sharingTitle)
            .putExtra(Intent.EXTRA_TEXT, url)
            .setType("text/plain")

        context?.startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.page_sharing_chooser_title)
            )
        )
    }

    override fun showLoading() {
        if (isAdded) {
            ibRefresh.startAnimation(rotationAnimation)
        }
    }

    override fun hideLoading() {
        if (isAdded) {
            ibRefresh.clearAnimation()
        }
    }

    override fun isActive(): Boolean = isAdded

    private fun setUpUI(rootView: View) {
        clDownloadedPopup = rootView.findViewById(R.id.clDownloadedPopup)
        clReaderTop = rootView.findViewById(R.id.clReaderTop)
        ibBack = rootView.findViewById(R.id.ibBack)
        ibDownload = rootView.findViewById(R.id.ibDownload)
        ibDownloadPopupClose = rootView.findViewById(R.id.ibDownloadPopupClose)
        ibRefresh = rootView.findViewById(R.id.ibRefresh)
        ibShare = rootView.findViewById(R.id.ibShare)
        layoutReaderBottom = rootView.findViewById(R.id.layoutReaderBottom)
        mtvBookTitle = rootView.findViewById(R.id.mtvBookTitle)
        mtvCurrentPage = rootView.findViewById(R.id.mtvCurrentPage)
        mtvDownloadTitle = rootView.findViewById(R.id.mtvDownloadTitle)
        rvQuickNavigation = rootView.findViewById(R.id.rvQuickNavigation)
        rvBookPages = rootView.findViewById(R.id.book_pages)
    }

    private fun requestStoragePermission() {
        val storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(storagePermission, REQUEST_STORAGE_PERMISSION)
    }

    private fun onPageChanged(position: Int) {
        presenter.updatePageIndicator(position)
        if (position - 1 >= 0) {
            bookReaderAdapter.resetPageToNormal(position - 1)
        }
        if (position + 1 < bookReaderAdapter.itemCount) {
            bookReaderAdapter.resetPageToNormal(position + 1)
        }
        rvQuickNavigation.scrollToPosition(position)
        navigationAdapter.updateSelectedIndex(position)
    }

    companion object {
        private const val TAG = "ReaderFragment"
        private const val REQUEST_STORAGE_PERMISSION = 2364
        private const val PRELOAD_SIZE = 3
    }
}
