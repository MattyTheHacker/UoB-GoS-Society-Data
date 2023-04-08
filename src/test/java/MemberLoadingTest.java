import members.Member;
import org.junit.Test;

import java.util.ArrayList;

import static members.GetMembers.*;

public class MemberLoadingTest {
    // test that members are saved to the files and loaded back in and the data is the same
    @Test
    public void testMemberSavingAndLoading() {
        // scrape members
        scrapeMembers();

        // put all members into temporary arraylist
        ArrayList<Member> tempMembers = new ArrayList<>(getAllMembers());

        // save members to files
        saveMembersToFile();

        // clear the members arraylist
        emptyMembersList();

        // load members from files
        loadMembersFromFile();

        // put new members into temporary arraylist
        ArrayList<Member> newMembers = new ArrayList<>(getAllMembers());

        // sort the two arraylists
        tempMembers.sort(Member::compareTo);
        newMembers.sort(Member::compareTo);

        System.out.println("[INFO] Members: " + tempMembers.size() + " " + newMembers.size());

        // compare the two arraylists
        assert tempMembers.equals(newMembers);
    }
}
