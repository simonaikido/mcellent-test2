package com.bim.seif.utils;

import com.bim.seif.dto.InstruccionResponse;
import com.bim.seif.models.Fideicomiso;
import com.bim.seif.models.FideicomisoConsulta;
import com.bim.seif.models.Region;
import com.bim.seif.models.jsonutilsmodels.ResponseJSONModel;
import com.bim.seif.models.jsonutilsmodels.UserRedisJSONModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Data
public class JSONUtils {


    static ResponseJSONModel response = new ResponseJSONModel();
    static UserRedisJSONModel userRedis = new UserRedisJSONModel();

    static public String responseGeneratorJSON(Boolean admin, String msg, String JWT, String codeAuth, String role) {

        response.setAdmin(admin);
        response.setMsg(msg);
        response.setJWT(JWT);
        response.setRole(role);
        response.setCodeAuth(codeAuth);
        Gson gson = new Gson();
        return gson.toJson(response);
    }

    static public String userRedisManage(String role, int attempts, String JWT, String codeAuth) {

        userRedis.setRole(role);
        userRedis.setAttempts(attempts);
        userRedis.setJwt(JWT);
        userRedis.setCodeAuth(codeAuth);
        Gson gson = new Gson();
        return gson.toJson(userRedis);
    }


    static public String GeneratorJSON(Boolean admin, String msg, String JWT, String role) {
        response.setAdmin(admin);
        response.setMsg(msg);
        response.setJWT(JWT);
        response.setRole(role);
        Gson gson = new Gson();
        return gson.toJson(response);
    }

    static public String covertObjectToJSON(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    static public UserRedisJSONModel covertJSONToUserRedisJSONModel(String strJSON, UserRedisJSONModel userRedis) {
        Gson gson = new Gson();
        return gson.fromJson(strJSON, userRedis.getClass());
    }

    static public Object covertJSONToObject(String strJSON, Object object) {
        Gson gson = new Gson();
        return gson.fromJson(strJSON, object.getClass());
    }

    public static List<Fideicomiso> convertJSONToArrayFideicomisos(String jsonFideicomisos, String emailCliente) {
        Gson gson = new Gson();

        // Define el tipo de lista que queremos convertir
        Type fideicomisoListType = new TypeToken<List<FideicomisoConsulta>>() {
        }.getType();

        // Convierte el JSON a una lista de Fideicomisos
        List<FideicomisoConsulta> fideicomisosConsulta = gson.fromJson(jsonFideicomisos, fideicomisoListType);

        return convertFideicomisoToFideicomisoConsulta(fideicomisosConsulta, emailCliente);
    }

    public static List<Fideicomiso> convertFideicomisoToFideicomisoConsulta(List<FideicomisoConsulta> fideicomisosConsulta, String emailCliente) {
        List<Fideicomiso> listaFideicomisos = new ArrayList<>();
        fideicomisosConsulta.forEach(fideicomisoConsulta -> {
            Fideicomiso fideicomiso = new Fideicomiso();
            fideicomiso.setFolio(fideicomisoConsulta.getFolio());
            fideicomiso.setAlias(fideicomisoConsulta.getAlias());
            Region region = new Region();
            region.setCve(fideicomiso.getRegion().getCve());
            fideicomiso.setNombreCliente(emailCliente);

            // Aquí puedes setear otros campos si es necesario
            listaFideicomisos.add(fideicomiso);
        });
        return listaFideicomisos;
    }

    public static String convertInstruccionesResponseToJSON(List<InstruccionResponse> instrucciones) {
        Gson gson = new Gson();

        return gson.toJson(instrucciones);
    }

}