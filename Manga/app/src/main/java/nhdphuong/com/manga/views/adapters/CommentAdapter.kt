package nhdphuong.com.manga.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_comment.view.advancedUserIcon
import kotlinx.android.synthetic.main.layout_comment.view.comment
import kotlinx.android.synthetic.main.layout_comment.view.commentDate
import kotlinx.android.synthetic.main.layout_comment.view.posterAvatar
import kotlinx.android.synthetic.main.layout_comment.view.posterName
import nhdphuong.com.manga.R
import nhdphuong.com.manga.api.ApiConstants
import nhdphuong.com.manga.data.entity.comment.Comment
import nhdphuong.com.manga.supports.ImageUtils
import nhdphuong.com.manga.views.becomeVisibleIf
import nhdphuong.com.manga.views.customs.MyTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    comments: List<Comment>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val commentList: ArrayList<Comment> = ArrayList(comments)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CommentViewHolder(parent)
    }

    override fun getItemCount(): Int = commentList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CommentViewHolder).bindTo(commentList[position])
    }

    fun addNewComments(comments: List<Comment>) {
        val oldItemCount = itemCount
        commentList.addAll(comments)
        notifyItemRangeInserted(oldItemCount, comments.size)
    }

    private class CommentViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.layout_comment, parent, false
        )
    ) {
        private val posterAvatar: ImageView = itemView.posterAvatar
        private val posterName: MyTextView = itemView.posterName
        private val content: MyTextView = itemView.comment
        private val dateTime: MyTextView = itemView.commentDate
        private val advancedUserIcon: ImageView = itemView.advancedUserIcon

        fun bindTo(comment: Comment) {
            comment.poster?.avatarUrl?.let {
                val imageUrl = ApiConstants.getCommentPosterAvatarUrl(it)
                ImageUtils.loadCircularImage(
                    imageUrl, R.drawable.ic_404_not_found, posterAvatar
                )
            }

            posterName.text = comment.poster?.userName.orEmpty()
            content.text = comment.body.orEmpty()

            comment.posDate?.let {
                val dateTimeFormat = SimpleDateFormat("E, dd MMM yyyy HH:mm", Locale.US)
                dateTime.text = dateTimeFormat.format(Date(it))
            }

            val isStaff = comment.poster?.isStaff ?: false
            val isSuperUser = comment.poster?.isSuperUser ?: false
            advancedUserIcon.becomeVisibleIf(isStaff || isSuperUser)
            when {
                isStaff -> advancedUserIcon.setImageResource(R.drawable.ic_staff_white)
                isSuperUser -> advancedUserIcon.setImageResource(R.drawable.ic_super_user)
            }
        }
    }
}
