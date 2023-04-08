package members;

import java.time.LocalDateTime;

public record Member(String name, int id, LocalDateTime joinDate, LocalDateTime expireDate) implements Comparable<Member> {

    @Override
    public int compareTo(Member o) {
        return this.id - o.id;
    }

    public static Member fromString(String decrypted) {
        // decrypted format: "Member[name=last name, firstname, id=id, joinDate=join date, expireDate=expire date]"
        // full name should be from the first equals sign to the second comma
        String fullName = decrypted.substring(decrypted.indexOf("=") + 1, decrypted.indexOf(",", decrypted.indexOf(",") + 1));
        int id = Integer.parseInt(decrypted.substring(decrypted.indexOf("id=") + 3, decrypted.indexOf(", joinDate")));
        LocalDateTime joinDate = LocalDateTime.parse(decrypted.substring(decrypted.indexOf("joinDate=") + 9, decrypted.indexOf(", expireDate")));
        LocalDateTime expireDate = LocalDateTime.parse(decrypted.substring(decrypted.indexOf("expireDate=") + 11, decrypted.indexOf("]")));
        return new Member(fullName, id, joinDate, expireDate);
    }

    public String getFirstName() {
        // name format: "Last, First"
        return name.substring(name.indexOf(",") + 2);
    }

    public String getLastName() {
        // name format: "Last, First"
        return name.substring(0, name.indexOf(","));
    }
}
