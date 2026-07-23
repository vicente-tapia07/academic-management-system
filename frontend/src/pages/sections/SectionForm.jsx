import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../services/api";

const DAY_OPTIONS = [
  { value: 1, label: "Lunes" },
  { value: 2, label: "Martes" },
  { value: 3, label: "Miércoles" },
  { value: 4, label: "Jueves" },
  { value: 5, label: "Viernes" },
  { value: 6, label: "Sábado" },
];

const BLOCK_OPTIONS = [
  { value: "08:15-09:35", label: "1° bloque  08:15 – 09:35" },
  { value: "09:50-11:10", label: "2° bloque  09:50 – 11:10" },
  { value: "11:25-12:45", label: "3° bloque  11:25 – 12:45" },
  { value: "13:45-15:05", label: "4° bloque  13:45 – 15:05" },
  { value: "15:20-16:40", label: "5° bloque  15:20 – 16:40" },
  { value: "16:55-18:15", label: "6° bloque  16:55 – 18:15" },
  { value: "18:45-20:05", label: "7° bloque  18:45 – 20:05" },
  { value: "20:05-21:25", label: "8° bloque  20:05 – 21:25" },
  { value: "21:25-22:45", label: "9° bloque  21:25 – 22:45" },
];

export default function SectionForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    subjectId: "",
    professorId: "",
    semesterId: "",
    roomId: "",
    dayOfWeek: "",
    block: "",
    totalSeats: "",
    availableSeats: "",
  });

  const [subjects, setSubjects] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [professors, setProfessors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const init = async () => {
      const [sRes, semRes, profRes, roomRes] = await Promise.all([
        api.get("/api/subjects"),
        api.get("/api/semesters"),
        api.get("/api/professors"),
        api.get("/api/rooms"),
      ]);
      setSubjects(sRes.data);
      setSemesters(semRes.data);
      setProfessors(profRes.data);
      setRooms(roomRes.data);

      // Si es edición, cargar datos actuales
      if (isEdit) {
        const secRes = await api.get(`/api/sections/${id}`);
        const s = secRes.data;
        const start = s.startTime?.slice(0, 5) ?? "";
        const end = s.endTime?.slice(0, 5) ?? "";
        setForm({
          subjectId: s.subjectId ?? "",
          professorId: s.professorId ?? "",
          semesterId: s.semesterId ?? "",
          roomId: s.roomId ?? "",
          dayOfWeek: s.dayOfWeek ?? "",
          block: start && end ? `${start}-${end}` : "",
          totalSeats: s.totalSeats ?? "",
          availableSeats: s.availableSeats ?? "",
        });
      }
    };
    init();
  }, [id, isEdit]);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleTotalSeats = (e) => {
    const val = e.target.value;
    setForm({ ...form, totalSeats: val, availableSeats: val });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.block) {
      setError("Selecciona un bloque horario.");
      return;
    }

    const [startTime, endTime] = form.block.split("-");
    setLoading(true);

    const payload = {
      subjectId: Number(form.subjectId),
      professorId: Number(form.professorId),
      semesterId: Number(form.semesterId),
      roomId: Number(form.roomId),
      dayOfWeek: Number(form.dayOfWeek),
      startTime,
      endTime,
      totalSeats: Number(form.totalSeats),
      availableSeats: Number(form.availableSeats),
    };

    try {
      if (isEdit) {
        await api.put(`/api/sections/${id}`, payload);
      } else {
        await api.post("/api/sections", payload);
      }
      navigate("/sections");
    } catch (err) {
      setError(
        err.response?.data ||
          err.response?.data?.message ||
          "Error al guardar la sección.",
      );
    } finally {
      setLoading(false);
    }
  };

  const statusLabel = (status) => {
    if (status === "IN_PROGRESS") return " ✅ En curso";
    if (status === "CLOSED") return " 🔒 Cerrado";
    return " 📅 Planificado";
  };

  return (
    <div className="container py-4" style={{ maxWidth: 600 }}>
      <div className="d-flex align-items-center gap-3 mb-4">
        <button
          className="btn btn-sm btn-outline-secondary"
          onClick={() => navigate("/sections")}
        >
          ← Volver
        </button>
        <div>
          <h2 className="fw-bold mb-0">
            {isEdit ? "Editar Sección" : "Nueva Sección"}
          </h2>
          <p className="text-muted mb-0">
            {isEdit ? `Modificando sección #${id}` : "Crea una nueva sección"}
          </p>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card shadow-sm border-0">
        <div className="card-body p-4">
          <form onSubmit={handleSubmit}>
            <div className="row g-3">
              <div className="col-12">
                <label className="form-label fw-semibold">Asignatura</label>
                <select
                  name="subjectId"
                  className="form-select"
                  value={form.subjectId}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Selecciona asignatura —</option>
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.code} — {s.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Semestre</label>
                <select
                  name="semesterId"
                  className="form-select"
                  value={form.semesterId}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Selecciona semestre —</option>
                  {semesters.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.year} — {s.period}
                      {statusLabel(s.status)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Profesor</label>
                <select
                  name="professorId"
                  className="form-select"
                  value={form.professorId}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Selecciona profesor —</option>
                  {professors.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.firstName} {p.lastName} — {p.department}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-12">
                <label className="form-label fw-semibold">Sala</label>
                <select
                  name="roomId"
                  className="form-select"
                  value={form.roomId}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Selecciona sala —</option>
                  {rooms.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.code} — {r.name} (cap. {r.capacity})
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-sm-5">
                <label className="form-label fw-semibold">Día</label>
                <select
                  name="dayOfWeek"
                  className="form-select"
                  value={form.dayOfWeek}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Día —</option>
                  {DAY_OPTIONS.map((d) => (
                    <option key={d.value} value={d.value}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="col-sm-7">
                <label className="form-label fw-semibold">Bloque horario</label>
                <select
                  name="block"
                  className="form-select"
                  value={form.block}
                  onChange={handleChange}
                  required
                >
                  <option value="">— Bloque —</option>
                  {BLOCK_OPTIONS.map((b) => (
                    <option key={b.value} value={b.value}>
                      {b.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Cupos con máximo dinámico */}
              <div className="col-sm-6">
                <label className="form-label fw-semibold">
                  Cupos Totales
                  {form.roomId && (
                    <span className="badge bg-info ms-2">
                      Max:{" "}
                      {rooms.find((r) => r.id === Number(form.roomId))
                        ?.capacity || "?"}
                    </span>
                  )}
                </label>
                <input
                  type="number"
                  name="totalSeats"
                  className="form-control"
                  min="1"
                  max={
                    form.roomId
                      ? rooms.find((r) => r.id === Number(form.roomId))
                          ?.capacity
                      : ""
                  }
                  value={form.totalSeats}
                  onChange={handleTotalSeats}
                  required
                />
              </div>

              <div className="col-sm-6">
                <label className="form-label fw-semibold">
                  Cupos Disponibles
                </label>
                <input
                  type="number"
                  name="availableSeats"
                  className="form-control"
                  min="0"
                  max={form.totalSeats}
                  value={form.availableSeats}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="d-flex gap-2 mt-4">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                {loading
                  ? "Guardando..."
                  : isEdit
                    ? "Guardar Cambios"
                    : "Crear Sección"}
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => navigate("/sections")}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
