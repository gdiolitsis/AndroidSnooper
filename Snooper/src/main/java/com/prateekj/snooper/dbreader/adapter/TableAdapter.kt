package com.prateekj.snooper.dbreader.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.prateekj.snooper.databinding.TableItemBinding

class TableAdapter(
    private val tableList: List<String>,
    private val tableEventListener: TableEventListener
) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    class TableViewHolder(
        private val binding: TableItemBinding,
        private val tableEventListener: TableEventListener
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            tableName: String,
            rowNum: Int
        ) {

            binding.tableName.text =
                tableName

            binding.rowNum.text =
                "$rowNum. "

            binding.root.setOnClickListener {

                tableEventListener.onTableClick(
                    tableName
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TableViewHolder {

        val binding =
            TableItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return TableViewHolder(
            binding,
            tableEventListener
        )
    }

    override fun onBindViewHolder(
        holder: TableViewHolder,
        position: Int
    ) {

        holder.bind(
            tableList[position],
            position + 1
        )
    }

    override fun getItemCount(): Int {

        return tableList.size
    }
}
