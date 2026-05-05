package usach.cl.demo.controller;

import usach.cl.demo.entity.Student;
import usach.cl.demo.model.StudentDto;
import usach.cl.demo.model.CurriculumDto;
import usach.cl.demo.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAll() {
        return ResponseEntity.ok(studentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable int id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Student> create(@RequestBody StudentDto dto) throws Exception{
        return ResponseEntity.ok(studentService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable int id, @RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/curriculum")
    public ResponseEntity<CurriculumDto> getCurriculum(@PathVariable int id) {
        return ResponseEntity.ok(studentService.getCurriculum(id));
    }
}