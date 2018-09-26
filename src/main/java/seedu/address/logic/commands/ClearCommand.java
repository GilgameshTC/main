package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import javafx.collections.ObservableList;
import seedu.address.logic.CommandHistory;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.ContactContainsRoomPredicate;
import seedu.address.model.person.ContactContainsTagPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.Room;

import java.util.ArrayList;
import java.util.List;

//@@author kengwoon
/**
 * Clears the address book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Clears all persons tagged with or under "
            + "the specified keywords(case-insensitive) or clears all persons for keyword 'all'.\n"
            + "Parameters: KEYWORD\n"
            + "Example: " + COMMAND_WORD + " all\n"
            + "Example: " + COMMAND_WORD + " basketball";
    public static final String MESSAGE_CLEAR_ALL_SUCCESS = "Hallper has been cleared!";
    public static final String MESSAGE_CLEAR_SPECIFIC_SUCCESS = "Cleared persons under %1$s in Hallper";
    public static final String MESSAGE_CLEAR_NOTHING = "No persons found under %1$s in Hallper";

    private final ContactContainsTagPredicate predicate;

    private final List<String> target;
    private ArrayList<Person> toClear;
    private List<Person> fullList;
    private boolean clearAll;
    private boolean clearRoom;

    public ClearCommand(List<String> target, ContactContainsTagPredicate predicate) {
        this.target = target;
        this.predicate = predicate;
        this.clearAll = false;
        this.clearRoom = false;
        this.toClear = new ArrayList<>();
        this.fullList = new ArrayList<>();
    }

    @Override
    public CommandResult execute(Model model, CommandHistory history) {
        requireNonNull(model);
        for (String s : target) {
            if (s.toLowerCase().equals("all")) {
                this.clearAll = true;
                break;
            }
            if (Room.isValidRoom(s)) {
                this.clearRoom = true;
                break;
            }
        }

        if (clearAll) {
            return clearAll(model);
        } else {
            return clearSpecific(model);
        }
    }

    private CommandResult clearAll(Model model) {
        model.resetData(new AddressBook());
        model.commitAddressBook();
        return new CommandResult(MESSAGE_CLEAR_ALL_SUCCESS);
    }


    private CommandResult clearSpecific(Model model) {
        toClear.clear();
        fullList = model.getAddressBook().getPersonList();
        for (Person p : fullList) {
            if (clearRoom ? new ContactContainsRoomPredicate(target).test(p): new ContactContainsTagPredicate(target).test(p)) {
                toClear.add(p);
            }
        }

        if (toClear.isEmpty()) {
            return new CommandResult(String.format(MESSAGE_CLEAR_NOTHING, target));
        }

        model.clearMultiplePersons(toClear);
        model.commitAddressBook();
        return new CommandResult(String.format(MESSAGE_CLEAR_SPECIFIC_SUCCESS, target));
    }
}
