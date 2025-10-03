package com.cursoandroid.ae2_abp1

import android.Manifest
import android.app.AlertDialog
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val contactViewModel: ContactViewModel by viewModels()
    private lateinit var contactAdapter: ContactAdapter
    private var pendingContact: Contact? = null
    private val REQUEST_CONTACT_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewContacts)
        contactAdapter = ContactAdapter(listOf())
        recyclerView.adapter = contactAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (hasContactPermissions()) {
            val systemContacts = getSystemContacts()
            contactViewModel.setContacts(systemContacts)
        } else {
            requestContactPermissions()
        }

        contactViewModel.contacts.observe(this, Observer { contacts ->
            contactAdapter = ContactAdapter(contacts)
            recyclerView.adapter = contactAdapter
        })

        supportFragmentManager.setFragmentResultListener("addContactResult", this) { _, bundle ->
            val name = bundle.getString("name") ?: return@setFragmentResultListener
            val phone = bundle.getString("phone") ?: return@setFragmentResultListener
            val contact = Contact(name, phone)
            if (hasContactPermissions()) {
                saveContactToSystem(contact)
                contactViewModel.addContact(contact)
            } else {
                pendingContact = contact
                requestContactPermissions()
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fabAddContact)
        fab.setOnClickListener {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                addToBackStack(null)
                replace(R.id.main, AddContactFragment())
            }
        }
    }

    private fun hasContactPermissions(): Boolean {
        val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return write && read
    }

    private fun requestContactPermissions() {
        val shouldShowRationaleWrite = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_CONTACTS)
        val shouldShowRationaleRead = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)
        if (shouldShowRationaleWrite || shouldShowRationaleRead) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS),
                REQUEST_CONTACT_PERMISSIONS
            )
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos necesarios")
            .setMessage("La app necesita permisos para leer y escribir contactos para poder guardar el nuevo contacto en tu agenda.")
            .setPositiveButton("Aceptar") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS),
                    REQUEST_CONTACT_PERMISSIONS
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACT_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                pendingContact?.let {
                    saveContactToSystem(it)
                    contactViewModel.addContact(it)
                    pendingContact = null
                }
            } else {
                val someDeniedPermanently = permissions.indices.any { index ->
                    grantResults[index] == PackageManager.PERMISSION_DENIED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[index])
                }
                if (someDeniedPermanently) {
                    showGoToSettingsDialog()
                } else {
                    Toast.makeText(this, "Permisos de contactos denegados. No se pudo guardar el contacto.", Toast.LENGTH_LONG).show()
                }
                pendingContact = null
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos denegados permanentemente")
            .setMessage("Debes habilitar los permisos de contactos manualmente en Ajustes para poder guardar contactos.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:" + packageName)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveContactToSystem(contact: Contact) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Toast.makeText(this, "Contacto guardado en el sistema", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar el contacto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getSystemContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: ""
                val phone = it.getString(numberIndex) ?: ""
                contacts.add(Contact(name, phone))
            }
        }
        return contacts
    }
}
