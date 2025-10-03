package com.cursoandroid.ae2_abp1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ContactViewModel : ViewModel() {
    private val _contacts = MutableLiveData<MutableList<Contact>>(mutableListOf())
    val contacts: LiveData<MutableList<Contact>> = _contacts

    fun addContact(contact: Contact) {
        _contacts.value?.add(contact)
        _contacts.value = _contacts.value
    }

    fun setContacts(contacts: List<Contact>) {
        _contacts.value = contacts.toMutableList()
    }
}
