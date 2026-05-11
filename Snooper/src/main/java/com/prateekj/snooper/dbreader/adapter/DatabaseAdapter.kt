package com.prateekj.snooper.dbreader.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.prateekj.snooper.databinding.DbCardItemBinding
import com.prateekj.snooper.dbreader.model.Database

class DatabaseAdapter(
    private val databaseList: List<Database>,
    private val dbEventListener: DbEventListener
) : RecyclerView.Adapter<DatabaseAdapter.DbViewHolder>() {

    class DbViewHolder(
        private val binding: DbCardItemBinding,
        private val dbEventListener: DbEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            db: Database
        ) {

            binding.name.text =
                db.name ?: ""

            binding.path.text =
                db.path ?: ""

            binding.root.setOnClickListener {

                dbEventListener.onDatabaseClick(db)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DbViewHolder {

        val binding =
            DbCardItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return DbViewHolder(
            binding,
            dbEventListener
        )
    }

    override fun onBindViewHolder(
        holder: DbViewHolder,
        position: Int
    ) {

        holder.bind(
            databaseList[position]
        )
    }

    override fun getItemCount(): Int {

        return databaseList.size
    }
}
