package com.example.todolistapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var tasks: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Listener untuk setiap aksi
    var onTaskCheckedListener: ((Task, Boolean) -> Unit)? = null
    var onFlowTimerClickListener: ((Task) -> Unit)? = null
    var onEditClickListener: ((Task) -> Unit)? = null
    var onDeleteClickListener: ((Task) -> Unit)? = null

    // Peta untuk warna prioritas (menggunakan color resource Anda)
    private val priorityColorMap = mapOf(
        "Low" to R.color.low_priority, // Pastikan warna ini ada di colors.xml
        "Medium" to R.color.medium_priority, // Pastikan warna ini ada di colors.xml
        "High" to R.color.high_priority // Pastikan warna ini ada di colors.xml
    )

    // Melacak item mana yang sedang di-expand
    private var expandedPosition = -1

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskItemContainer: View = itemView.findViewById(R.id.taskItemContainer)
        val title: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val priorityIcon: ImageView = itemView.findViewById(R.id.ivPriority)
        val time: TextView = itemView.findViewById(R.id.tvTaskTime)
        val category: TextView = itemView.findViewById(R.id.tvTaskCategory)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkboxTask)
        val arrowToggle: ImageView = itemView.findViewById(R.id.ivArrowToggle)

        val actionButtonsContainer: LinearLayout = itemView.findViewById(R.id.actionButtonsContainer)
        val btnFlowTimer: Button = itemView.findViewById(R.id.btnFlowTimer)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        init {
            taskItemContainer.setOnClickListener {
                // Update posisi yang di-expand
                val previousExpandedPosition = expandedPosition
                expandedPosition = if (adapterPosition == expandedPosition) -1 else adapterPosition

                // Beri tahu adapter untuk menggambar ulang item yang lama dan yang baru
                notifyItemChanged(previousExpandedPosition)
                notifyItemChanged(expandedPosition)
            }

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedListener?.invoke(tasks[adapterPosition], isChecked)
            }

            btnFlowTimer.setOnClickListener { onFlowTimerClickListener?.invoke(tasks[adapterPosition]) }
            btnEdit.setOnClickListener { onEditClickListener?.invoke(tasks[adapterPosition]) }
            btnDelete.setOnClickListener { onDeleteClickListener?.invoke(tasks[adapterPosition]) }
        }

        fun bind(task: Task, isExpanded: Boolean) {
            val context = itemView.context

            title.text = task.title

            // Format waktu menggunakan dueDate dari Firestore
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            time.text = sdf.format(task.dueDate.toDate())

            category.text = task.category

            if (task.priority != "None" && task.priority.isNotEmpty()) {
                priorityIcon.visibility = View.VISIBLE
                val colorResId = priorityColorMap[task.priority] ?: R.color.dark_blue
                priorityIcon.setColorFilter(ContextCompat.getColor(context, colorResId))
            } else {
                priorityIcon.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = task.status == "completed"
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedListener?.invoke(task, isChecked)
            }

            // Atur tampilan expand/collapse
            actionButtonsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowToggle.rotation = if (isExpanded) 180f else 0f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isExpanded = position == expandedPosition
        holder.bind(task, isExpanded)
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        expandedPosition = -1 // Reset expand state saat data baru masuk
        notifyDataSetChanged()
    }
}