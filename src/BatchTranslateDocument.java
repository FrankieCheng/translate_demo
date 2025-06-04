import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.translate.v3.BatchDocumentInputConfig;
import com.google.cloud.translate.v3.BatchDocumentOutputConfig;
import com.google.cloud.translate.v3.BatchTranslateDocumentMetadata;
import com.google.cloud.translate.v3.BatchTranslateDocumentRequest;
import com.google.cloud.translate.v3.BatchTranslateDocumentResponse;
import com.google.cloud.translate.v3.GcsDestination;
import com.google.cloud.translate.v3.GcsSource;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.longrunning.Operation;
import com.google.longrunning.OperationsClient;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class BatchTranslateDocument {

  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    String projectId = "replace_your_project_id";
    // 输入文件的GCS URI，例如：gs://your-bucket/path/to/your/documents/*
    String gcsInputUri = "gs://your-bucket/path/to/your/documents/*";
    // 输出文件的GCS URI，例如：gs://your-bucket/path/to/your/results/
    String gcsOutputUri = "gs://your-bucket/path/to/your/results/";
    // 源语言代码，例如 "en" 代表英语
    String sourceLang = "en";
    // 目标语言代码，例如 "zh-CN" 代表简体中文
    String targetLang = "zh-CN";

    batchTranslateDocument(projectId, gcsInputUri, gcsOutputUri, sourceLang, targetLang);
  }

  /**
   * 使用Cloud Translation API批量翻译GCS中的文档
   *
   * @param projectId    你的Google Cloud项目ID
   * @param gcsInputUri  包含待翻译文档的GCS路径 (e.g., "gs://bucket-name/input/")
   * @param gcsOutputUri 存放翻译结果的GCS路径 (e.g., "gs://bucket-name/output/")
   * @param sourceLang   文档的源语言代码 (e.g., "en")
   * @param targetLang   要翻译成的目标语言代码 (e.g., "zh-CN")
   * @throws IOException          如果发生I/O错误
   * @throws ExecutionException   如果操作执行失败
   * @throws InterruptedException 如果线程在等待时被中断
   */
  public static void batchTranslateDocument(
      String projectId,
      String gcsInputUri,
      String gcsOutputUri,
      String sourceLang,
      String targetLang)
      throws IOException, ExecutionException, InterruptedException {

    try (TranslationServiceClient client = TranslationServiceClient.create()) {

      LocationName parent = LocationName.of(projectId, "us-central1");

      // ... 输入和输出配置部分不变 ...
      GcsSource gcsSource = GcsSource.newBuilder().setInputUri(gcsInputUri).build();
      BatchDocumentInputConfig inputConfig = BatchDocumentInputConfig.newBuilder()
          .setGcsSource(gcsSource)
          .build();
      GcsDestination gcsDestination = GcsDestination.newBuilder().setOutputUriPrefix(gcsOutputUri).build();
      BatchDocumentOutputConfig outputConfig = BatchDocumentOutputConfig.newBuilder()
          .setGcsDestination(gcsDestination)
          .build();


      BatchTranslateDocumentRequest request =
          BatchTranslateDocumentRequest.newBuilder()
              .setParent(parent.toString())
              .setSourceLanguageCode(sourceLang)
              .addTargetLanguageCodes(targetLang)
              .addInputConfigs(inputConfig)
              .setOutputConfig(outputConfig)
              .setEnableShadowRemovalNativePdf(true)
              .build();

      System.out.println("🚀 提交批量翻译请求...");
      // 异步调用 API
      OperationFuture<BatchTranslateDocumentResponse, BatchTranslateDocumentMetadata> future =
          client.batchTranslateDocumentAsync(request);

      // 不调用 .get() 来等待，而是直接获取操作名称
      String operationName = future.getName();

      while (true) {
        // 获取用于管理操作的客户端
        OperationsClient operationsClient = client.getOperationsClient();

        System.out.printf("🔍 正在检查操作: %s\n", operationName);

        // 调用 getOperation 获取最新状态
        Operation operation = operationsClient.getOperation(operationName);

        // 检查 done 字段
        if (operation.getDone()) {
          System.out.println("🎉 操作已完成！");

          // 检查操作是成功还是失败
          if (operation.hasError()) {
            Status error = operation.getError();
            System.err.printf("❌ 操作失败: [%d] %s\n", error.getCode(), error.getMessage());
          } else if (operation.hasResponse()) {
            System.out.println("✅ 操作成功。");
            try {
              // 从响应中解包出具体的结果
              BatchTranslateDocumentResponse response =
                  operation.getResponse().unpack(BatchTranslateDocumentResponse.class);

              System.out.println("--- 翻译结果摘要 ---");
              System.out.printf("总页数: %d\n", response.getTotalPages());
              System.out.printf("翻译成功的页数: %d\n", response.getTranslatedPages());
              System.out.printf("失败的页数: %d\n", response.getFailedPages());
              System.out.printf("总字符数: %d\n", response.getTotalCharacters());
              System.out.printf("翻译成功的字符数: %d\n", response.getTranslatedCharacters());
              System.out.printf("结果已存入: %s\n", request.getOutputConfig().getGcsDestination().getOutputUriPrefix());

            } catch (InvalidProtocolBufferException e) {
              System.err.println("❌ 解析响应失败: " + e.getMessage());
            }
          }
          break;
        } else {
          System.out.println("⏳ 操作仍在进行中，请稍后重试...");
          Thread.sleep(5000);
        }
      }
    }
  }
}