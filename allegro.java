import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Optional;

public class Main {

    public static final String START_OBJECT="{";
    public static final String END_OBJECT="}";
    public static final String QUOTE="\"";
    public static final String COLON=":";
    public static final String SEPARATOR=",";
    public static final String END=""; // created for fun


    static class JsonObject{
        private String json;

        public JsonObject(String json){
            this.json=json;
        }

        public String json(){
            return this.json;
        }

        public static JsonObject createMapper(Class<?> clazz){
            return null;
        }

        public String findValueByFieldName(final String fieldName, final String jsonValue){
            int fieldIndex = jsonValue.indexOf(fieldName);

            StringBuilder result = new StringBuilder();

            for (int i = fieldIndex+fieldName.length() + 3; i < jsonValue.length(); i ++){
                String charAtI = String.valueOf(jsonValue.charAt(i));

                boolean isNextCharacter = !charAtI.equals(QUOTE) && !charAtI.equals(SEPARATOR) && !charAtI.equals(END_OBJECT);
                if(isNextCharacter){
                    result.append(charAtI);
                }else{
                    return result.toString();
                }
            }
            return null;
        }
    }

    static class SimpleJsonOperator{
        private final StringBuilder jsonBuilder = new StringBuilder();

        public void clearJson(){
            this.jsonBuilder.delete(0,jsonBuilder.length()-1);
        }

        public void startObject(){
            jsonBuilder.append(START_OBJECT);
        }
        public void endObject(){
            jsonBuilder.append(END_OBJECT);
        }


        public void addField(String fieldName, Object val, boolean isLast){
            jsonBuilder.append(QUOTE).append(fieldName)
                    .append(QUOTE)
                    .append(COLON)
                    .append(QUOTE)
                    .append(val)
                    .append(QUOTE)
                    .append(isLast ? END : SEPARATOR);
        }

        public String getJson(){
            String content = jsonBuilder.toString();
            this.jsonBuilder.delete(0,jsonBuilder.length()-1);

            return content;
        }
    }
    static class TokenResponse{
        private String token;
        public TokenResponse(String token){
            this.token=token;
        }
        public Optional<String> getToken(){return Optional.ofNullable(this.token);}
        public void setToken(String token){this.token=token;}

        public String bearer(){
            return "Bearer ".concat(this.token);
        }

    }

    static class Allegro{
        private static final HttpClient CLIENT = HttpClient.newBuilder().build();
        private static TokenResponse tokenResponse;
        private static final String CLIENT_ID="your-client-id-here";
        private static final String CLIENT_SECRET="your-client-secret-here";
        private static final String TOKEN_URL="https://allegro.pl.allegrosandbox.pl/auth/oauth/token";
        private static final String BASE_ENDPOINT="https://api.allegro.pl.allegrosandbox.pl";
        private static final String AUTHORIZATION_HEADER="Authorization";
        private static final String ACCEPT="Accept";
        private static final String ACCEPT_ALLEGRO_API="application/vnd.allegro.public.v1+json";

        private static void assertTokenNotNull(){
            boolean notPresent = tokenResponse.getToken().isEmpty();
            if(notPresent) throw new RuntimeException("Token is not present");
        }

        public static void fetchAccessToken(final String grantType){

            String encodedAuthorization = "Basic ".concat(Base64.getEncoder().encodeToString(CLIENT_ID.concat(COLON).concat(CLIENT_SECRET).getBytes()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL.concat("?grant_type=").concat("client_credentials")))
                    .headers("Authorization",encodedAuthorization)
                    .build();
            try{
                HttpResponse<String> jsonResponse = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                int responseStatus=jsonResponse.statusCode();
                boolean isOk = responseStatus>=200;

                if(isOk){
                    String json = jsonResponse.body();

                    if(json!=null){
                        JsonObject jsonObject = new JsonObject(json);
                        String token =  jsonObject.findValueByFieldName("access_token", json);

                        if(token!=null)
                            tokenResponse = new TokenResponse(token);
                    }
                }
            }catch (IOException | InterruptedException e){
                e.printStackTrace();
            }
        }

        private static HttpRequest baseRequest(URI uri){
            return HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .header(AUTHORIZATION_HEADER, tokenResponse.bearer())
                    .header(ACCEPT,ACCEPT_ALLEGRO_API)
                    .build();
        }

        public static Optional<JsonObject> fetchSaleCategories() throws IOException, InterruptedException {
            assertTokenNotNull();

            URI endpoint=URI.create(BASE_ENDPOINT.concat("/sale/categories"));
            HttpRequest request = baseRequest(endpoint);

            HttpResponse<String> sellers = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            boolean isOkey = sellers.statusCode() >= 200 && sellers.statusCode() < 300;

            String jsonRsponse = sellers.body();

            if(isOkey){
                boolean responseNotNull=jsonRsponse!=null;
                if(responseNotNull)
                    return Optional.of(new JsonObject(jsonRsponse));
            }
            return Optional.empty();
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Allegro.fetchAccessToken("client_credentials");
        Allegro.fetchSaleCategories()
                .ifPresentOrElse(response->{
                    System.out.println(response.json());
                },()-> System.out.println("could not fetch"));
    }
}
