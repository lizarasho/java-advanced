package ru.ifmo.rain.rasho.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SuppressWarnings("unused")
public class StudentDB implements AdvancedStudentGroupQuery {

    private final Comparator<Student> NAME_ID_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .thenComparing(Student::compareTo);


    private Stream<Map.Entry<String, List<Student>>> splitStudentsIntoGroups(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    private List<Group> getCollectedByFunctionGroups(Collection<Student> students,
                                                     Function<? super List<Student>, List<Student>> function) {
        return splitStudentsIntoGroups(students)
                .map(e -> new Group(e.getKey(), function.apply(e.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getCollectedByFunctionGroups(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getCollectedByFunctionGroups(students, this::sortStudentsById);
    }

    private String getLargestByFunctionGroup(Collection<Student> students,
                                             Function<? super List<Student>, Integer> function) {
        return splitStudentsIntoGroups(students)
                .map(e -> Map.entry(e.getKey(), function.apply(e.getValue())))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestByFunctionGroup(students, List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestByFunctionGroup(students, e -> getDistinctFirstNames(e).size());
    }

    private <T extends Collection<String>> T mapToCollection(List<Student> students,
                                                             Function<Student, String> function,
                                                             Supplier<T> collection) {
        return students.stream().map(function).collect(Collectors.toCollection(collection));
    }

    private List<String> mapToList(List<Student> students, Function<Student, String> function) {
        return mapToCollection(students, function, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    private String getFullName(Student student) {
        return student.getFirstName() + ' ' + student.getLastName();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }


    private List<Student> sortStudentsToList(Collection<Student> students, Comparator<? super Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsToList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsToList(students, NAME_ID_COMPARATOR
        );
    }

    private List<Student> findStudents(Collection<Student> students,
                                       Function<Student, String> filteredFunction,
                                       String filteredString) {
        return students.stream()
                .sorted(NAME_ID_COMPARATOR)
                .filter(s -> filteredFunction.apply(s).equals(filteredString))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudents(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudents(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudents(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findStudentsByGroup(students, group).stream()
                .collect(
                        Collectors.toMap(
                                Student::getLastName,
                                Student::getFirstName,
                                BinaryOperator.minBy(String::compareTo))
                );
    }

    private List<String> getByIndices(Collection<Student> students,
                                      int[] indices,
                                      Function<? super List<Student>, List<String>> function) {
        List<String> list = function.apply(new ArrayList<>(students));
        return Arrays.stream(indices).mapToObj(list::get).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, this::getFirstNames);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, this::getLastNames);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, this::getGroups);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getByIndices(students, indices, this::getFullNames);
    }

    @Override
    public String getMostPopularName(Collection<Student> collection) {
        return collection.stream()
                .collect(
                        Collectors.groupingBy(
                                this::getFullName,
                                HashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        set -> set.stream().map(Student::getGroup).distinct().count()
                                )
                        )
                )
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry<String, Long>::getValue).thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).orElse("");
    }
}