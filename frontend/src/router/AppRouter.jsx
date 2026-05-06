import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

import Navbar from '../components/Navbar';
import PrivateRoute from '../components/PrivateRoute';

import LoginPage         from '../pages/LoginPage';
import AdminDashboard    from '../pages/AdminDashboard';
import CareerList        from '../pages/careers/CareerList';
import CareerForm        from '../pages/careers/CareerForm';
import SubjectList       from '../pages/subjects/SubjectList';
import SubjectForm       from '../pages/subjects/SubjectForm';
import SectionList       from '../pages/sections/SectionList';
import SectionForm       from '../pages/sections/SectionForm';
import SemesterList      from '../pages/semesters/SemesterList';
import SemesterForm      from '../pages/semesters/SemesterForm';
import SemesterClose     from '../pages/semesters/SemesterClose';
import StudentList       from '../pages/students/StudentList';
import StudentCurriculum from '../pages/students/StudentCurriculum';
import FailureReport     from '../pages/reports/FailureReport';
import StudentDashboard   from '../pages/students/StudentDashboard';
import StudentProfile     from '../pages/students/StudentProfile';
import StudentEnrollments from '../pages/students/StudentEnrollments';
import EnrollForm         from '../pages/students/EnrollForm';

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

        <Route element={<PrivateRoute roles={['ROLE_ADMIN']} />}>
          <Route path="/dashboard"               element={<AdminDashboard />} />
          <Route path="/careers"                 element={<CareerList />} />
          <Route path="/careers/new"             element={<CareerForm />} />
          <Route path="/careers/edit/:id"        element={<CareerForm />} />
          <Route path="/subjects"                element={<SubjectList />} />
          <Route path="/subjects/new"            element={<SubjectForm />} />
          <Route path="/subjects/edit/:id"       element={<SubjectForm />} />
          <Route path="/sections"                element={<SectionList />} />
          <Route path="/sections/new"            element={<SectionForm />} />
          <Route path="/semesters"               element={<SemesterList />} />
          <Route path="/semesters/new"           element={<SemesterForm />} />
          <Route path="/semesters/edit/:id"      element={<SemesterForm />} />
          <Route path="/semesters/close/:id"     element={<SemesterClose />} />
          <Route path="/students"                element={<StudentList />} />
          <Route path="/students/:id/curriculum" element={<StudentCurriculum />} />
          <Route path="/reports"                 element={<FailureReport />} />
        </Route>

        <Route element={<PrivateRoute roles={['ROLE_STUDENT', 'ROLE_ADMIN']} />}>
          <Route path="/my-dashboard"   element={<StudentDashboard />}   />
          <Route path="/my-curriculum"  element={<StudentCurriculum />}  />
          <Route path="/my-enrollments" element={<StudentEnrollments />} />
          <Route path="/my-enroll"      element={<EnrollForm />}         />
          <Route path="/my-profile"     element={<StudentProfile />}     />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}