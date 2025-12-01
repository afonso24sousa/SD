package sd.traffic.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;

/**
 * Classe responsável por carregar ficheiros de configuração JSON.
 *
 * Esta classe abstrai a leitura de ficheiros JSON usando a biblioteca Gson.
 * É usada em várias fases do projeto para carregar parâmetros de simulação,
 * políticas de semáforos e rotas de veículos.
 *
 * Exemplos de ficheiros lidos:
 *  - src/main/resources/config/default_config.json
 *  - src/main/resources/config/policy_hybrid.json
 *  - src/main/resources/config/routes.json
 */
public class ConfigLoader {

    // Caminhos padrão usados em todo o projeto
    public static final String DEFAULT_CONFIG_PATH = "src/main/resources/config/default_config.json";
    public static final String POLICY_CONFIG_PATH   = "src/main/resources/config/policy_hybrid.json";
    public static final String ROUTES_CONFIG_PATH   = "src/main/resources/config/routes.json";

    /**
     * Lê um ficheiro JSON e converte o conteúdo num objeto JsonObject.
     *
     * @param path Caminho para o ficheiro JSON (relativo ou absoluto)
     * @return Objeto JsonObject representando a estrutura do ficheiro
     * @throws RuntimeException caso o ficheiro não exista ou não possa ser lido
     */
    public static JsonObject load(String path) {
        try (FileReader reader = new FileReader(path)) {
            // Converte o ficheiro JSON num objeto genérico (árvore JSON)
            return new Gson().fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException("Erro a ler ficheiro: " + path, e);
        }
    }

    /**
     * Atalho para carregar o ficheiro de configuração principal da simulação.
     */
    public static JsonObject loadDefaultConfig() {
        return load(DEFAULT_CONFIG_PATH);
    }

    /**
     * Atalho para carregar o ficheiro de política de semáforos.
     */
    public static JsonObject loadPolicyConfig() {
        return load(POLICY_CONFIG_PATH);
    }

    /**
     * Atalho para carregar o ficheiro de rotas.
     */
    public static JsonObject loadRoutesConfig() {
        return load(ROUTES_CONFIG_PATH);
    }

    /**
     * Método de teste rápido para validar a leitura de ficheiros JSON.
     *
     * Permite verificar se o Gson está a funcionar e se os caminhos estão corretos.
     * Deve ser executado apenas para debug — não faz parte da simulação.
     */
    public static void main(String[] args) {
        JsonObject config = loadDefaultConfig();
        System.out.println("Config carregada com sucesso:");
        System.out.println(config);
    }
}
