package com.correios.edwinos.consultacorreios;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.correios.edwinos.consultacorreios.util.Dialog;
import com.correios.edwinos.consultacorreios.util.database.CorreiosDataBase;
import com.correios.edwinos.consultacorreios.util.database.CorreiosEntity;
import com.correios.edwinos.consultacorreios.util.database.Entity;
import com.correios.edwinos.consultacorreios.util.json.JsonParser;
import com.correios.edwinos.consultacorreios.util.list.ItemListModel;
import com.correios.edwinos.consultacorreios.util.list.ListAdapter;


public class MainActivity extends ListActivity implements Dialog.DialogResult {

    protected String preAddedCode;
    protected String preAddedName;
    protected String preAddedJson;

    protected CorreiosDataBase correiosObjectsData;

    public static final int INSERT_ACTION = 1;
    public static final int VERIFY_ACTION = 2;
    public static final int VIEW_DATA = 3;

    public static final int INSERT_QUESTION = 10;
    public static final int RESET_QUESTION = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.correiosObjectsData = new CorreiosDataBase(this, "com.correios.edwinos.consultacorreios.util.database.CorreiosEntity");

        this.update();
    }

    protected void onListItemClick(android.widget.ListView l, android.view.View v, int position, long id){
        ListAdapter adapter = (ListAdapter) l.getAdapter();

        Intent dataViewIntent = new Intent("com.correios.edwinos.consultacorreios.DataViewActivity");
        dataViewIntent.putExtra("code", adapter.getItem(position).getCode());
        startActivityForResult(dataViewIntent, MainActivity.VIEW_DATA);
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
            return true;
        }

        if(id == R.id.action_reset){

            Dialog.questionDialog(this, RESET_QUESTION, "Remover tudo", "Você tem certeza que deseja remover todos os itens da lista?");
            return true;
        }

        if(id == R.id.action_about){
            Dialog.alertDialog(this, "Sobre", "Aplicativo desenvolvido por:\nEdwino Stein - edwino.stein@ufrr.br\n");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        switch (requestCode) {
            case INSERT_ACTION:
                if(resultCode == RESULT_OK) {
                    this.preAddedName = data.getStringExtra("name");
                    this.preAddedCode = data.getStringExtra("code");

                    this.verifyCode(this.preAddedCode);
                }
            break;

            case VERIFY_ACTION:

                if(resultCode == RESULT_OK) {
                    this.preAddedJson = data.getStringExtra("response");
                    JsonParser jsonResponse = new JsonParser(this.preAddedJson);

                    if(!jsonResponse.isSuccess()){
                        this.resetPreAdd();
                        Dialog.alertDialog(this, "Houve um problema na consulta", jsonResponse.getMessage());
                    }
                    else if (jsonResponse.getTotal() <= 0){
                        Dialog.questionDialog(this, INSERT_QUESTION, "Nenhuma informação sobre o objeto", "Nenhuma informação sobre o rastreamento do objeto foi encontrada pelos correios.\nÉ possivel que o objeto ainda não tenha entrado no sistema dos correios.\n\nDeseja mesmo assim adiciona-lo a lista?" );
                    }else {
                        this.confirmAdd();
                    }
                }
                else{
                    this.resetPreAdd();
                    Dialog.alertDialog(this, "Houve um problema na consulta", "A comunicação com o servidor falhou.\n\nVerifique se você está conectado a internet.");
                }
            break;

            case VIEW_DATA:
                this.update();
            break;
        }
    }

    protected void update(){

        ListAdapter adapter = new ListAdapter(this);
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

    protected void resetPreAdd(){
        this.preAddedCode = null;
        this.preAddedName = null;
        this.preAddedJson = null;
    }

    public void confirmAdd(){
        CorreiosEntity newObject = new CorreiosEntity();

        newObject.setCode(this.preAddedCode);
        newObject.setName(this.preAddedName);
        newObject.setJson_data(this.preAddedJson);

        if(!this.correiosObjectsData.insert(newObject)){
            Dialog.alertDialog(this, "Erro ao Registrar Objeto", this.getError(this.correiosObjectsData.getErrorMessage()));
        }
        else {
            newObject = (CorreiosEntity) this.correiosObjectsData.select("code='"+this.preAddedCode+"'")[0];
            this.update();
        }

        this.resetPreAdd();
    }

    protected void verifyCode(String code){

        Intent requestIntent = new Intent("com.correios.edwinos.consultacorreios.RequestActivity");
        requestIntent.putExtra("code", code);

        startActivityForResult(requestIntent, MainActivity.VERIFY_ACTION);
    }


    protected String getError(String errorMessage){
        if(errorMessage.equals("column code is not unique (code 19)")){
            return "O objeto com código \""+this.preAddedCode+"\" já está cadastrado!";
        }
        else {
            return "Erro na pessistencia do banco de dados";
        }
    }

    @Override
    public void onDialogResult(int index, boolean result) {
        switch (index){
            case INSERT_QUESTION:
                if(result){
                    confirmAdd();
                } else{
                    resetPreAdd();
                }
            break;

            case RESET_QUESTION:
                if(result){
                    this.correiosObjectsData.clearAll();
                    ((ListAdapter) this.getListAdapter()).clear();
                }
            break;
        }
    }
}
