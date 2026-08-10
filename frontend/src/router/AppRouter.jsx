import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

import Navbar from "../components/Navbar";
import PrivateRoute from "../components/PrivateRoute";

import LoginPage from "../pages/LoginPage";
import AdminDashboard from "../pages/AdminDashboard";
import CareerList from "../pages/careers/CareerList";
import CareerForm from "../pages/careers/CareerForm";
import SubjectList from "../pages/subjects/SubjectList";
import SubjectForm from "../pages/subjects/SubjectForm";
import SectionList from "../pages/sections/SectionList";
import SectionForm from "../pages/sections/SectionForm";
import SemesterList from "../pages/semesters/SemesterList";
import SemesterForm from "../pages/semesters/SemesterForm";
import SemesterClose from "../pages/semesters/SemesterClose";
import StudentList from "../pages/students/StudentList";
import StudentCurriculum from "../pages/students/StudentCurriculum";
import StudentForm from "../pages/students/StudentForm";
import StudentEnrollAdmin from "../pages/students/StudentEnrollAdmin";
import FailureReport from "../pages/reports/FailureReport";
import StudentDashboard from "../pages/students/StudentDashboard";
import StudentProfile from "../pages/students/StudentProfile";
import StudentEnrollments from "../pages/students/StudentEnrollments";
import EnrollForm from "../pages/students/EnrollForm";
import StudentCourses from "../pages/students/StudentCourses";
import StudentSchedule from "../pages/students/StudentSchedule";
import ProfessorDashboard from "../pages/professor/ProfessorDashboard";
import ProfessorCourses from "../pages/professor/ProfessorCourses";
import ProfessorSchedule from "../pages/professor/ProfessorSchedule";
import StudentGrades from "../pages/professor/StudentGrades";
import GradeForm from "../pages/professor/GradeForm";
import MyGrades from "../pages/students/MyGrades";
import StudentCertificate from "../pages/students/StudentCertificate";
import ProfessorList from "../pages/professor/ProfessorList";
import ProfessorForm from "../pages/professor/ProfessorForm";
import StudentEnrollmentsAdmin from "../pages/students/StudentEnrollmentsAdmin";
import ProfessorCoursesAdmin from "../pages/professor/ProfessorCoursesAdmin";

function Layout({ children }) {
  const { user } = useAuth();
  return (
    <>
      {user && <Navbar />}
      <main className="py-2">{children}</main>
    </>
  );
}

export default function AppRouter() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<LoginPage />} />

        {/* ── ADMIN ── */}
        <Route element={<PrivateRoute roles={["ROLE_ADMIN"]} />}>
          <Route path="/dashboard" element={<AdminDashboard />} />
          <Route path="/careers" element={<CareerList />} />
          <Route path="/careers/new" element={<CareerForm />} />
          <Route path="/careers/edit/:id" element={<CareerForm />} />
          <Route path="/subjects" element={<SubjectList />} />
          <Route path="/subjects/new" element={<SubjectForm />} />
          <Route path="/subjects/edit/:id" element={<SubjectForm />} />
          <Route path="/sections" element={<SectionList />} />
          <Route path="/sections/new" element={<SectionForm />} />
          <Route path="/sections/edit/:id" element={<SectionForm />} />
          <Route path="/semesters" element={<SemesterList />} />
          <Route path="/semesters/new" element={<SemesterForm />} />
          <Route path="/semesters/edit/:id" element={<SemesterForm />} />
          <Route path="/semesters/close/:id" element={<SemesterClose />} />
          <Route path="/students" element={<StudentList />} />
          <Route path="/students/new" element={<StudentForm />} />
          <Route path="/students/edit/:id" element={<StudentForm />} />
          <Route
            path="/students/:id/curriculum"
            element={<StudentCurriculum />}
          />
          <Route path="/students/enroll" element={<StudentEnrollAdmin />} />
          <Route
            path="/students/:id/enrollments"
            element={<StudentEnrollmentsAdmin />}
          />
          <Route path="/professors" element={<ProfessorList />} />
          <Route path="/professors/new" element={<ProfessorForm />} />
          <Route path="/professors/edit/:id" element={<ProfessorForm />} />
          <Route path="/professors/:id/courses" element={<ProfessorCoursesAdmin />} />
        </Route>

        {/* ── ESTUDIANTE ── */}
        <Route
          element={<PrivateRoute roles={["ROLE_STUDENT", "ROLE_ADMIN"]} />}
        >
          <Route path="/my-dashboard" element={<StudentDashboard />} />
          <Route path="/my-curriculum" element={<StudentCurriculum />} />
          <Route path="/my-courses" element={<StudentCourses />} />
          <Route path="/my-schedule" element={<StudentSchedule />} />
          <Route path="/my-enrollments" element={<StudentEnrollments />} />
          <Route path="/my-enroll" element={<EnrollForm />} />
          <Route path="/my-profile" element={<StudentProfile />} />
          <Route path="/my-grades" element={<MyGrades />} />
          <Route path="/my-certificate" element={<StudentCertificate />} />
        </Route>

        {/* ── PROFESOR ── */}
        <Route element={<PrivateRoute roles={["ROLE_PROFESSOR"]} />}>
          <Route path="/professor" element={<ProfessorDashboard />} />
          <Route path="/professor/courses" element={<ProfessorCourses />} />
          <Route path="/professor/schedule" element={<ProfessorSchedule />} />
          <Route
            path="/professor/grades/:sectionId"
            element={<StudentGrades />}
          />
          <Route path="/professor/grade/new" element={<GradeForm />} />
        </Route>

        {/* ── ADMIN + PROFESOR ── */}
        <Route
          element={<PrivateRoute roles={["ROLE_ADMIN", "ROLE_PROFESSOR"]} />}
        >
          <Route path="/reports" element={<FailureReport />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
