package uk.gov.hmcts.dm.functional

import io.restassured.response.Response
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner
import org.junit.Test
import org.junit.runner.RunWith
import uk.gov.hmcts.dm.functional.utilities.Classifications
import uk.gov.hmcts.dm.functional.utilities.V1MediaTypes

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo

@RunWith(SpringIntegrationSerenityRunner.class)
class MultiMediaUploadIT extends BaseIT {

    @Test
    void "MV1 (R1) As authenticated user I upload whitelisted multi media files"() {
        uploadWhitelistedFileThenDownload("video_test.mp4", "video/mp4")

        // larger sizes video file
        uploadWhitelistedFileThenDownload("27_MB_video_mp4", "video/mp4")
        uploadWhitelistedFileThenDownload("video_file_58MB.mp4", "video/mp4")

        uploadWhitelistedFileThenDownload("audio_test.mp3", "audio/mpeg")
    }

    @Test
    void "MV1 (R1) As authenticated user I cannot upload not whitelisted multi media files"() {
        uploadNotWhitelistedFileThenDownload("video_test.mov", "video/quicktime")
        uploadNotWhitelistedFileThenDownload("video_test.avi", "video/x-msvideo")
        uploadNotWhitelistedFileThenDownload("video_test.mpg", "video/mpeg")
        uploadNotWhitelistedFileThenDownload("video_test.webm", "video/webm")
        uploadNotWhitelistedFileThenDownload("video_test.wmv", "video/x-ms-wmv")

        uploadNotWhitelistedFileThenDownload("audio_test.wav", "audio/vnd.wave")
        uploadNotWhitelistedFileThenDownload("audio_test.aac", "audio/x-aac")
        uploadNotWhitelistedFileThenDownload("audio_test.ogg", "audio/vorbis")
        uploadNotWhitelistedFileThenDownload("audio_test.wma", "audio/x-ms-wma")

    }

    private uploadWhitelistedFileThenDownload(String filename, String mimeType) {
        Response response = givenRequest(CITIZEN)
            .multiPart("files", file(filename), mimeType)
            .multiPart("classification", Classifications.PUBLIC as String)
            .multiPart("roles", "citizen")
            .multiPart("roles", "caseworker")
            .expect().log().all()
            .statusCode(200)
            .contentType(V1MediaTypes.V1_HAL_DOCUMENT_COLLECTION_MEDIA_TYPE_VALUE)

            .body("_embedded.documents[0].originalDocumentName", equalTo(filename))
            .body("_embedded.documents[0].mimeType", equalTo(mimeType))
            .body("_embedded.documents[0].classification", equalTo(Classifications.PUBLIC as String))
            .body("_embedded.documents[0].roles[0]", equalTo("caseworker"))
            .body("_embedded.documents[0].roles[1]", equalTo("citizen"))
            .when()
            .post("/documents")

        String documentUrl1 = response.path("_embedded.documents[0]._links.self.href")
        String documentContentUrl1 = response.path("_embedded.documents[0]._links.binary.href")

        givenRequest(CITIZEN)
            .expect()
            .statusCode(200)
            .contentType(V1MediaTypes.V1_HAL_DOCUMENT_MEDIA_TYPE_VALUE)
            .body("originalDocumentName", equalTo(filename))
            .body("classification", equalTo(Classifications.PUBLIC as String))
            .body("roles[0]", equalTo("caseworker"))
            .body("roles[1]", equalTo("citizen"))
            .when()
            .get(documentUrl1)

        assertByteArrayEquality filename, givenRequest(CITIZEN)
            .expect()
            .statusCode(200)
            .contentType(containsString(mimeType))
            .header("OriginalFileName", filename)
            .when()
            .get(documentContentUrl1)
            .asByteArray()
    }

    private uploadNotWhitelistedFileThenDownload(String filename, String mimeType) {
        Response response = givenRequest(CITIZEN)
            .multiPart("files", file(filename), mimeType)
            .multiPart("classification", Classifications.PUBLIC as String)
            .multiPart("roles", "citizen")
            .multiPart("roles", "caseworker")
            .expect().log().all()
            .statusCode(422)
            .body("error", equalTo("Your upload contains a disallowed file type"))
            .when()
            .post("/documents")
    }

}
