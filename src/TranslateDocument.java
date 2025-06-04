import com.google.cloud.translate.v3.DocumentInputConfig;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateDocumentRequest;
import com.google.cloud.translate.v3.TranslateDocumentResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TranslateDocument {

  public static void main(String[] args) throws IOException {
    // TODO(developer): 请替换为你的实际信息
    String projectId = "replace_your_project_id";
    // 源语言代码，留空或设为 "auto" 可让API自动检测
    String sourceLang = "en";
    // 目标语言代码
    String targetLang = "zh-CN";
    // 本地待翻译文件的路径, /Users/frankie/Downloads/XXXX.pdf
    String inputFilePath = "replace_your_input_file";
    // 翻译后文件保存的路径,/Users/frankie/Downloads/translated_XXXX.pdf
    String outputFilePath = "replace_your_output_file";
    // 文档的 MIME 类型
    String mimeType = "application/pdf";

    translateDocument(
        projectId, sourceLang, targetLang, inputFilePath, outputFilePath, mimeType);
  }

  /**
   * 同步翻译单个本地文档
   *
   * @param projectId      你的 Google Cloud 项目 ID
   * @param sourceLang     源语言代码 (e.g., "en")
   * @param targetLang     目标语言代码 (e.g., "zh-CN")
   * @param inputFilePath  本地输入文件的路径
   * @param outputFilePath 翻译后文件的保存路径
   * @param mimeType       输入文件的 MIME 类型 (e.g., "application/pdf")
   * @throws IOException 如果文件读写发生错误
   */
  public static void translateDocument(
      String projectId,
      String sourceLang,
      String targetLang,
      String inputFilePath,
      String outputFilePath,
      String mimeType)
      throws IOException {

    // 初始化翻译服务客户端
    try (TranslationServiceClient client = TranslationServiceClient.create()) {

      // 准备父级位置
      LocationName parent = LocationName.of(projectId, "global");

      System.out.printf("🔄 正在读取文件: %s\n", inputFilePath);
      // 读取本地文件内容为字节数组
      byte[] fileContent = Files.readAllBytes(Paths.get(inputFilePath));

      // 将字节数组转换为 ByteString
      ByteString content = ByteString.copyFrom(fileContent);

      // 构建文档输入配置，将文件内容和MIME类型包含进去
      DocumentInputConfig documentInputConfig =
          DocumentInputConfig.newBuilder()
              .setContent(content)
              .setMimeType(mimeType)
              .build();

      // 构建翻译请求
      TranslateDocumentRequest request =
          TranslateDocumentRequest.newBuilder()
              .setParent(parent.toString())
              .setTargetLanguageCode(targetLang)
              // 源语言是可选的，API可以自动检测
              .setSourceLanguageCode(sourceLang)
              .setDocumentInputConfig(documentInputConfig)
              .build();

      System.out.println("🚀 正在发送翻译请求，请稍候...");

      // 发送同步请求并获取响应
      TranslateDocumentResponse response = client.translateDocument(request);

      System.out.println("✅ 翻译完成！");

      // 从响应中获取翻译后的文档内容
      ByteString translatedContent = response.getDocumentTranslation().getByteStreamOutputs(0);

      // 将翻译后的内容写入新的本地文件
      Files.write(Paths.get(outputFilePath), translatedContent.toByteArray());

      System.out.printf("💾 翻译后的文件已保存到: %s\n", outputFilePath);
    }
  }
}