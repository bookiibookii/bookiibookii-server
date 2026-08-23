package com.example.bookiibookii.domain.aladin.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AladinClientTest {

    @Test
    void searchesBooksUsingOfficialSearchTargetAndParsesNormalResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AladinClient client = new AladinClient(builder);
        ReflectionTestUtils.setField(client, "aladinKey", "test-key");

        server.expect(requestTo("https://www.aladin.co.kr/ttb/api/ItemSearch.aspx"
                        + "?ttbkey=test-key&Query=spring&start=1&MaxResults=10&SearchTarget=Book"
                        + "&cover=Big&output=JS&Version=20131101"))
                .andExpect(queryParam("SearchTarget", "Book"))
                .andRespond(withSuccess("""
                        {
                          "totalResults": 1,
                          "item": [{"title": "Spring Book", "isbn13": "9780000000001"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        AladinClient.AladinItemSearchResponse response =
                client.searchBooksByKeyword("spring", 1, 10);

        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.item()).singleElement()
                .satisfies(item -> assertThat(item.title()).isEqualTo("Spring Book"));
        server.verify();
    }
}
