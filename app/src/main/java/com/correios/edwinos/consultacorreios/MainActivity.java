package com.correios.edwinos.consultacorreios;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.correios.edwinos.consultacorreios.util.database.CorreiosDataBase;
import com.correios.edwinos.consultacorreios.util.database.CorreiosEntity;
import com.correios.edwinos.consultacorreios.util.database.Entity;
import com.correios.edwinos.consultacorreios.util.json.JsonParser;
import com.correios.edwinos.consultacorreios.util.list.ItemListModel;
import com.correios.edwinos.consultacorreios.util.list.ListAdapter;


public class MainActivity extends ListActivity {
    protected ItemListModel preAdded;
    protected CorreiosDataBase correiosObjectsData;

    public static final int INSERT_ACTION = 1;
    public static final int VERIFY_ACTION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListAdapter adapter = new ListAdapter(this);
        this.correiosObjectsData = new CorreiosDataBase(this, "com.correios.edwinos.consultacorreios.util.database.CorreiosEntity");

        Entity[] data = this.correiosObjectsData.fetchAll();
        if(data != null && data.length > 0){

            Log.d("Events", "Dados retornados do banco: " + data.length);
            for (int i = 0; i < data.length; i++){
                Log.d("Events", "Item: "+i+"; Id: "+((CorreiosEntity) data[i]).getId()+"; Code"+((CorreiosEntity) data[i]).getCode());
                adapter.add(new ItemListModel((CorreiosEntity) data[i]));
            }

        }
        else{
            Log.d("Events", "Banco de dados vazio");
        }

        setListAdapter(adapter);
    }

    protected void onListItemClick(android.widget.ListView l, android.view.View v, int position, long id){
        ListAdapter adapter = (ListAdapter) l.getAdapter();
        Toast.makeText(this, "Selecionado: " + adapter.getItem(position).getFrendlyName()+" - "+adapter.getItem(position).getId(), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_add){
            startActivityForResult(new Intent("com.correios.edwinos.consultacorreios.InsertActivity"), MainActivity.INSERT_ACTION);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        switch (requestCode) {
            case INSERT_ACTION:
                if(resultCode == RESULT_OK) {
                    this.preAdded = new ItemListModel(data.getStringExtra("code"), data.getStringExtra("name"));
                    this.verifyCode(this.preAdded.getCode());
                }
            break;
            case VERIFY_ACTION:

                if(resultCode == RESULT_OK) {
                    JsonParser jsonResponse = new JsonParser(data.getStringExtra("response"));

                    if(!jsonResponse.isSuccess()){
                        this.preAdded = null;
                        this.showErrorDialog("Houve um problema na consulta", jsonResponse.getMessage());
                    }
                    else if (jsonResponse.getTotal() <= 0){
                        this.showDataEmptyDialog();

                    }else {
                        this.confirmAdd();
                    }
                }
                else{
                    this.preAdded = null;
                    this.showErrorDialog("Houve um problema na consulta", "A comunicação com o servidor falhou.\n\nVerifique se você está conectado a internet.");
                }


            break;
        }
    }

    public void confirmAdd(){
        ((ListAdapter) this.getListAdapter()).add(this.preAdded);
        this.preAdded = null;
    }

    protected void verifyCode(String code){

        Intent requestIntent = new Intent("com.correios.edwinos.consultacorreios.RequestActivity");
        requestIntent.putExtra("code", code);

        startActivityForResult(requestIntent, MainActivity.VERIFY_ACTION);
    }


    protected void showDataEmptyDialog(){
        new AlertDialog.Builder(this).setTitle("Nenhuma informação sobre o objeto")
                                     .setMessage("Nenhuma informação sobre o rastreamento do objeto foi encontrada pelos correios.\nÉ possivel que o objeto ainda não tenha entrado no sistema dos correios.\n\nDeseja mesmo assim adiciona-lo a lista?")
                                     .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            confirmAdd();
                                        }
                                     })
                                     .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                                         public void onClick(DialogInterface dialog, int which) {
                                             preAdded = null;
                                             dialog.cancel();
                                         }
                                     })
                                     .show();
    }

    protected void showErrorDialog(String title, String textBody){
        new AlertDialog.Builder(this).setTitle(title)
                                     .setMessage(textBody)
                                     .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                         public void onClick(DialogInterface dialog, int which) {
                                             dialog.cancel();
                                         }
                                     })
                                     .show();
    }
}
