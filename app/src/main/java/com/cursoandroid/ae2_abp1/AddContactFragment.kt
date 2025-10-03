package com.cursoandroid.ae2_abp1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

class AddContactFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextPhone = view.findViewById<EditText>(R.id.editTextPhone)
        val buttonSave = view.findViewById<Button>(R.id.buttonSaveContact)

        buttonSave.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                parentFragmentManager.setFragmentResult("addContactResult", bundleOf(
                    "name" to name,
                    "phone" to phone
                ))
                parentFragmentManager.popBackStack()
            } else {
                editTextName.error = if (name.isEmpty()) "Requerido" else null
                editTextPhone.error = if (phone.isEmpty()) "Requerido" else null
            }
        }
    }
}
