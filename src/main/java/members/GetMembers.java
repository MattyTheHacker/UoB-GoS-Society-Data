package members;

import io.github.cdimascio.dotenv.Dotenv;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

import static encryption.EncryptionHandler.*;
import static utils.FileHandler.SanitiseFileName;
import static utils.FileHandler.listFiles;

public class GetMembers {
    private static final Dotenv dotenv = Dotenv.load();


    private static final String SOC_MEMBER_LIST_URL = dotenv.get("SOC_MEMBER_LIST_URL");
    private static final String SOC_COMMITTEE_COOKIE = dotenv.get("SOC_COMMITTEE_COOKIE");
    private static final String MEMBER_TABLE_ID = dotenv.get("MEMBER_TABLE_ID");

    private static final ArrayList<Member> members = new ArrayList<>();

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static boolean isMemberFromId(int id) {
        for (Member member : members) {
            if (member.id() == id) {
                return true;
            }
        }
        return false;
    }

    public static HttpResponse<String> getMembersPage() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(SOC_MEMBER_LIST_URL))
                .header("Cookie", SOC_COMMITTEE_COOKIE)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response;
        } else {
            System.out.println("[ERROR] Failed to get members page with error code " + response.statusCode() + ".");
            return null;
        }
    }

    public static void getmembersFromPage(HttpResponse<String> response) {
        Document doc = Jsoup.parse(response.body());
        Elements memberRows = Objects.requireNonNull(doc.getElementById(MEMBER_TABLE_ID)).getElementsByTag("tr");
        for (int i = 1; i < memberRows.size(); i++) {
            Elements memberData = memberRows.get(i).getElementsByTag("td");

            try {
                int id = Integer.parseInt(memberData.get(1).text());
                String name = memberData.get(0).text();
                LocalDateTime joinDate = LocalDateTime.parse(memberData.get(2).text(), dtf);
                LocalDateTime expiryDate = LocalDateTime.parse(memberData.get(3).text(), dtf);

                members.add(new Member(name, id, joinDate, expiryDate));
                System.out.printf("[INFO] Added member %s with ID %d.%n", name, id);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Failed to parse member ID.");
                System.out.println("[DEBUG] members.Member Data: " + memberData);
            } catch (DateTimeParseException e){
                System.out.println("[ERROR] Failed to parse member join date or expiry date.");
                System.out.println("[DEBUG] members.Member Data: " + memberData);
            }
        }
    }

    private static void scrapeMembers() {
        HttpResponse<String> response;
        try {
            response = getMembersPage();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response != null) {
            getmembersFromPage(response);
        }
    }

    private static void saveMembersToFile() {
        // encrypt the members arraylist and save it to a file
        for (Member member : members) {
            // encrypt the member ID to use as a filename
            String encryptedId = encryptId(member.id());

            // sanitise the encrypted ID to remove any invalid characters
            String filename = SanitiseFileName(encryptedId);

            // set the filepath
            String filepath = "data/" + filename + ".member";

            // encode the member to a file
            encodeMemberToFile(member, filepath);
        }
    }

    private static void loadMembersFromFile() {
        System.out.println("[DEBUG] Loading members from files...");
        // load the members from their files
        // search the data directory for files ending in .member
        // for each file, decrypt the file and add the member to the members arraylist
        Set<String> files = listFiles("data");
        System.out.println("[DEBUG] Found files: " + files);
        for (String file : files) {
            if (file.endsWith(".member")) {
                String filename = "data/" + file;
                Member member = decodeMembersFromFile(filename);
                if (member != null) {
                    members.add(member);
                } else {
                    System.out.println("[ERROR] Failed to load member from file " + file + ".");
                }
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Society members.Member List Scraper!");
        System.out.print("Would you like to scrape or reload? (s/r)");
        String scrape = scanner.nextLine();

        if (scrape.equalsIgnoreCase("s")) {
            scrapeMembers();
        } else if (scrape.equalsIgnoreCase("r")) {
            loadMembersFromFile();
        } else {
            System.out.println("Invalid option.");
            return;
        }

        if (members.size() == 0) {
            System.out.println("No members found.");
            return;
        } else {
            System.out.println("[INFO] Found " + members.size() + " members.");
            // print out all members
            for (Member member : members) {
                System.out.println(member);
            }
        }

        System.out.print("Would you like to save the members to a file? (y/n)");
        String save = scanner.nextLine();

        if (save.equalsIgnoreCase("y")) {
            saveMembersToFile();
        }
    }
}
