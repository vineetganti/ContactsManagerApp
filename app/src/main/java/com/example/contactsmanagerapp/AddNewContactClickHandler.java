package com.example.contactsmanagerapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

public class AddNewContactClickHandler {

    Contacts contact;
    Context context;
    MyViewModel myViewModel;

    public AddNewContactClickHandler(Contacts contact, Context context, MyViewModel myViewModel) {
        this.contact = contact;
        this.context = context;
        this.myViewModel = myViewModel;
    }

    public void onSubmitBtnClicked(View view) {
        if (contact.getName() == null || contact.getEmail() == null){
            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
        }
        else {
            myViewModel.addNewContact(contact);
            Intent i = new Intent(view.getContext(), com.example.contactsmanagerapp.MainActivity.class);
            i.putExtra("name", contact.getName());
            i.putExtra("Email", contact.getEmail());
            context.startActivity(i);
        }
    }

}
