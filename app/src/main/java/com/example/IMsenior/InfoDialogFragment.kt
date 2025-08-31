package com.example.IMsenior

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class InfoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val title = args.getString(ARG_TITLE) ?: "標題"
        val message = args.getString(ARG_MESSAGE) ?: "內容"

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message) // 直接放字串 (AlertDialog 內建會自帶 ScrollView)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        fun newInstance(title: String, message: String): InfoDialogFragment {
            val fragment = InfoDialogFragment()
            val bundle = Bundle()
            bundle.putString(ARG_TITLE, title)
            bundle.putString(ARG_MESSAGE, message)
            fragment.arguments = bundle
            return fragment
        }
    }
}
