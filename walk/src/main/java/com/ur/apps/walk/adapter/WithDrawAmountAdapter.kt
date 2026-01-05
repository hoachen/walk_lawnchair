package com.ur.apps.walk.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sg.response.FormField
import com.ur.apps.walk.R

/**
 * 提现金额表单字段RecyclerView适配器
 * 支持三种视图类型：电话输入、选择框、普通输入
 */
class WithDrawAmountAdapter(
    private val onFieldValueChanged: (FormField, String) -> Unit
) : ListAdapter<FormField, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    var spinnerListener: AdapterView.OnItemSelectedListener? = null

    companion object {
        private const val VIEW_TYPE_PHONE = 1
        private const val VIEW_TYPE_SELECT = 2
        private const val VIEW_TYPE_NORMAL = 3

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FormField>() {
            override fun areItemsTheSame(oldItem: FormField, newItem: FormField): Boolean {
                return oldItem.fieldKey == newItem.fieldKey
            }

            override fun areContentsTheSame(oldItem: FormField, newItem: FormField): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val field = getItem(position)
        return when (field.type) {
            FormField.PHONE_NUMBER -> VIEW_TYPE_PHONE
            FormField.SELECT -> VIEW_TYPE_SELECT
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PHONE -> PhoneViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.with_draw_input_item_phone, parent, false)
            )

            VIEW_TYPE_SELECT -> SelectViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.with_draw_input_item_select, parent, false)
            )

            else -> NormalViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.with_draw_input_item_normal, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val field = getItem(position)
        when (holder) {
            is PhoneViewHolder -> holder.bind(field, onFieldValueChanged)
            is SelectViewHolder -> holder.bind(field, onFieldValueChanged)
            is NormalViewHolder -> holder.bind(field, onFieldValueChanged)
        }
    }

    /**
     * 更新单个字段的值
     */
    fun updateField(updatedField: FormField) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.fieldKey == updatedField.fieldKey }
        if (index != -1) {
            currentList[index] = updatedField
            submitList(currentList)
        }
    }

    /**
     * 批量更新字段
     */
    fun updateFields(newFields: List<FormField>) {
        submitList(newFields)
    }

    /**
     * 电话输入ViewHolder
     */
    inner class PhoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val etPhone: EditText = itemView.findViewById(R.id.etPhone)
        private val tvCountryCode: TextView = itemView.findViewById(R.id.tvCountryCode)
        private val countryCodeLayout: LinearLayout = itemView.findViewById(R.id.countryCodeLayout)
        private val label: TextView = itemView.findViewById(R.id.label)

        fun bind(field: FormField, onFieldValueChanged: (FormField, String) -> Unit) {
            val labelText = field.label ?: ""
            if (labelText.isEmpty()) {
                label.visibility = View.GONE
            } else {
                label.visibility = View.VISIBLE
                label.text = labelText
            }
            etPhone.hint =
                field.placeholder?.let {
                    it.ifEmpty { itemView.context.getString(R.string.enter_phone) }
                }
            // 设置默认值
            val defaultValue = field.defaultValue ?: ""
            if (defaultValue.isNotEmpty()) {
                etPhone.setText(field.defaultValue)
            }
            // 设置输入监听
            etPhone.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                }

                override fun afterTextChanged(s: Editable?) {
                    onFieldValueChanged.invoke(field, "${etPhone.text}")
                }
            })
            // 国家码选择点击
            countryCodeLayout.setOnClickListener {
                // 这里可以触发国家码选择对话框
                // 暂时留空，由Activity处理
            }
            //默认设置key没有填写，方便检查
            onFieldValueChanged.invoke(
                field, ""
            )
        }
    }

    /**
     * 选择框ViewHolder
     */
    inner class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val etDocument: EditText = itemView.findViewById(R.id.etDocument)
        private val spinnerDocumentType: Spinner = itemView.findViewById(R.id.spinnerDocumentType)
        private val label: TextView = itemView.findViewById(R.id.label)

        fun bind(field: FormField, onFieldValueChanged: (FormField, String) -> Unit) {
            val labelText = field.label ?: ""
            if (labelText.isEmpty()) {
                label.visibility = View.GONE
            } else {
                label.visibility = View.VISIBLE
                label.text = labelText
            }

            // 设置提示文本
            etDocument.hint =
                field.placeholder?.let {
                    it.ifEmpty { itemView.context.getString(R.string.enter_name) }
                }
            // 设置默认值
            val defaultValue = field.defaultValue ?: ""
            if (defaultValue.isNotEmpty()) {
                etDocument.setText(field.defaultValue)
            }
            // 设置输入监听
            etDocument.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                }

                override fun afterTextChanged(s: Editable?) {
                    onFieldValueChanged.invoke(field, etDocument.text.toString())
                }
            })
            val displayList = field.options.map {
                it.displayText
            }

            val adapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                displayList
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerDocumentType.tag = field.options
            spinnerDocumentType.adapter = adapter
            spinnerDocumentType.onItemSelectedListener = spinnerListener
            onFieldValueChanged.invoke(
                field, field.options.firstOrNull()?.value ?: ""
            )
        }
    }

    /**
     * 普通输入ViewHolder
     */
    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val etName: EditText = itemView.findViewById(R.id.etName)
        private val label: TextView = itemView.findViewById(R.id.label)

        fun bind(field: FormField, onFieldValueChanged: (FormField, String) -> Unit) {
            val labelText = field.label ?: ""
            if (labelText.isEmpty()) {
                label.visibility = View.GONE
            } else {
                label.visibility = View.VISIBLE
                label.text = labelText
            }

            // 设置提示文本
            etName.hint =
                field.placeholder?.let {
                    it.ifEmpty { itemView.context.getString(R.string.enter_name) }
                }
            // 设置默认值
            val defaultValue = field.defaultValue ?: ""
            if (defaultValue.isNotEmpty()) {
                etName.setText(field.defaultValue)
            }
            // 设置输入监听
            etName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                }

                override fun afterTextChanged(s: Editable?) {
                    onFieldValueChanged.invoke(field, etName.text.toString())
                }
            })
            //默认设置key没有填写，方便检查
            onFieldValueChanged.invoke(
                field, ""
            )
        }
    }
}
