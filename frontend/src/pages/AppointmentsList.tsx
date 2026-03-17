import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { AppointmentApi, Appointment, PatientApi, PatientProfile } from "../api";
import { HiCalendar, HiPlus, HiClock, HiUser, HiX, HiCheckCircle } from "react-icons/hi";
import PatientDetailModal from "../components/appointment/PatientDetailModal";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { getImageUrl } from "../utils/imageUtils";
import { getAppointmentStatusColor, sortAppointments, formatAppointmentDateTime, getDisplayStatus, isAppointmentPassed } from "../utils/appointmentUtils";
import { getStartOfToday, getEndOfWeek, getStartOfDate } from "../utils/dateUtils";

export default function AppointmentsList() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPatient, setSelectedPatient] = useState<PatientProfile | null>(null);
  const [loadingPatient, setLoadingPatient] = useState(false);
  const [patientProfiles, setPatientProfiles] = useState<Map<number, PatientProfile>>(new Map());

  useEffect(() => {
    fetchAppointments();
  }, []);

  useEffect(() => {
    if (user?.role === "DOCTOR" && appointments.length > 0) {
      fetchPatientProfiles();
    }
  }, [appointments, user?.role]);

  const fetchPatientProfiles = async () => {
    const uniquePatientIds = [...new Set(appointments.map(apt => apt.patientId))];
    const profilesMap = new Map<number, PatientProfile>(patientProfiles);
    
    const missingIds = uniquePatientIds.filter(id => !profilesMap.has(id));
    
    for (const patientId of missingIds) {
      try {
        const profile = await PatientApi.getProfileById(patientId);
        profilesMap.set(patientId, profile);
      } catch (err: any) {
        // Silently fail - will retry when user clicks on patient
      }
    }
    
    setPatientProfiles(profilesMap);
  };

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await AppointmentApi.getMyAppointments();
      setAppointments(data);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load appointments");
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (id: number) => {
    if (!window.confirm("Are you sure you want to cancel this appointment?")) {
      return;
    }

    try {
      await AppointmentApi.cancelAppointment(id);
      fetchAppointments();
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to cancel appointment");
    }
  };

  const handlePatientClick = async (patientId: number) => {
    try {
      setLoadingPatient(true);
      setError(null);
      const patient = await PatientApi.getProfileById(patientId);
      setSelectedPatient(patient);
      setPatientProfiles(prev => new Map(prev).set(patientId, patient));
    } catch (err: any) {
      const cachedPatient = patientProfiles.get(patientId);
      if (cachedPatient) {
        setSelectedPatient(cachedPatient);
      } else {
        const appointment = appointments.find(apt => apt.patientId === patientId);
        if (appointment) {
          const nameParts = appointment.patientName.split(' ');
          const fallbackPatient = {
            id: patientId,
            firstName: nameParts[0] || '',
            lastName: nameParts.slice(1).join(' ') || '',
          } as PatientProfile;
          setSelectedPatient(fallbackPatient);
        } else {
          const errorMessage = err.response?.data?.message || 
                              err.response?.data?.error || 
                              err.message || 
                              "Failed to load patient information.";
          setError(errorMessage);
        }
      }
    } finally {
      setLoadingPatient(false);
    }
  };


  if (loading) {
    return (
      <div className="p-6 flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-white mb-2">Appointments</h1>
          <p className="text-gray-400">Manage your appointments</p>
        </div>
        {user?.role === "PATIENT" && (
          <button
            onClick={() => navigate("/appointments/book")}
            className="btn-primary flex items-center space-x-2"
          >
            <HiPlus className="w-5 h-5" />
            <span>Book Appointment</span>
          </button>
        )}
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400">
          {error}
        </div>
      )}

      {appointments.length === 0 ? (
        <div className="card text-center py-12">
          <HiCalendar className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-white mb-2">No Appointments</h3>
          <p className="text-gray-400 mb-6">
            {user?.role === "PATIENT" 
              ? "You don't have any appointments yet. Book your first appointment!"
              : "You don't have any appointments scheduled yet."}
          </p>
          {user?.role === "PATIENT" && (
            <button
              onClick={() => navigate("/appointments/book")}
              className="btn-primary"
            >
              Book Appointment
            </button>
          )}
        </div>
      ) : user?.role === "DOCTOR" ? (
        <DoctorAppointmentsView
          appointments={appointments}
          handlePatientClick={handlePatientClick}
          handleCancel={handleCancel}
          loadingPatient={loadingPatient}
          patientProfiles={patientProfiles}
        />
      ) : (
        <PatientAppointmentsView
          appointments={appointments}
          handleCancel={handleCancel}
        />
      )}

      {selectedPatient && (
        <PatientDetailModal
          patient={selectedPatient}
          onClose={() => setSelectedPatient(null)}
        />
      )}
    </div>
  );
}

function DoctorAppointmentsView({
  appointments,
  handlePatientClick,
  handleCancel,
  loadingPatient,
  patientProfiles,
}: {
  appointments: Appointment[];
  handlePatientClick: (patientId: number) => void;
  handleCancel: (id: number) => void;
  loadingPatient: boolean;
  patientProfiles: Map<number, PatientProfile>;
}) {
  const sortedAppointments = sortAppointments(appointments);
  const today = getStartOfToday();
  const endOfWeek = getEndOfWeek();

  const todaysAppointments = sortedAppointments.filter((apt) => {
    const aptDate = getStartOfDate(apt.appointmentDateTime);
    return apt.status !== "CANCELLED" && 
           apt.status !== "COMPLETED" && 
           aptDate.getTime() === today.getTime();
  });

  const thisWeeksAppointments = sortedAppointments.filter((apt) => {
    const aptDate = new Date(apt.appointmentDateTime);
    return apt.status !== "CANCELLED" && 
           apt.status !== "COMPLETED" && 
           aptDate >= today && 
           aptDate <= endOfWeek &&
           aptDate.getTime() !== today.getTime();
  });

  const upcomingAppointments = sortedAppointments.filter(
    (apt) => apt.status !== "CANCELLED" && apt.status !== "COMPLETED" && new Date(apt.appointmentDateTime) >= new Date()
  );


  return (
    <div className="space-y-6">
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <h2 className="text-2xl font-bold text-white mb-4">Today's Appointments</h2>
        <p className="text-gray-400 mb-6">
          You have {todaysAppointments.length} {todaysAppointments.length === 1 ? "appointment" : "appointments"} remaining today
        </p>

        <div className="space-y-3">
          {todaysAppointments.length === 0 ? (
            <p className="text-gray-400 text-center py-8">No appointments scheduled for today</p>
          ) : (
            todaysAppointments.map((appointment) => {
              const { date, time } = formatAppointmentDateTime(appointment.appointmentDateTime);
              
              return (
                <div
                  key={appointment.id}
                  className="bg-gray-700/50 rounded-lg p-4 border border-gray-600 hover:border-gray-500 transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-4 flex-1">
                      <div className="flex-shrink-0">
                        {(() => {
                          const patient = patientProfiles.get(appointment.patientId);
                          const profilePicture = patient ? getImageUrl(patient.profilePicture) : null;
                          const initials = patient 
                            ? `${patient.firstName?.[0] || ""}${patient.lastName?.[0] || ""}`
                            : appointment.patientName.split(' ').map(n => n[0]).join('').slice(0, 2);
                          
                          return (
                            <Avatar className="w-12 h-12 border-2 border-gray-600 flex-shrink-0">
                              <AvatarImage src={profilePicture || undefined} alt={appointment.patientName} />
                              <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-sm">
                                {initials}
                              </AvatarFallback>
                            </Avatar>
                          );
                        })()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <button
                          onClick={() => handlePatientClick(appointment.patientId)}
                          disabled={loadingPatient}
                          className="text-left hover:text-blue-400 transition-colors w-full"
                        >
                          <h3 className="text-lg font-semibold text-white mb-1 hover:underline">
                            {appointment.patientName}
                          </h3>
                        </button>
                        {(() => {
                          const patient = patientProfiles.get(appointment.patientId);
                          if (patient) {
                            return (
                              <div className="text-xs text-gray-400 mb-2">
                                {patient.dateOfBirth && (
                                  <span>DOB: {new Date(patient.dateOfBirth).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}</span>
                                )}
                                {patient.gender && (
                                  <span className="ml-2">• {patient.gender}</span>
                                )}
                              </div>
                            );
                          }
                          return null;
                        })()}
                        {appointment.notes && (
                          <p className="text-sm text-gray-300 mb-2">
                            <span className="text-gray-400">Reason: </span>
                            {appointment.notes}
                          </p>
                        )}
                        <div className="flex items-center space-x-4 text-sm text-gray-400">
                          <span>{date}</span>
                          <span>{time}</span>
                          <span className={`px-2 py-1 rounded text-xs ${getAppointmentStatusColor(appointment.status)}`}>
                            {appointment.status}
                          </span>
                        </div>
                      </div>
                    </div>
                    {appointment.status !== "CANCELLED" && appointment.status !== "COMPLETED" && (
                      <button
                        onClick={() => handleCancel(appointment.id)}
                        className="ml-4 px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 transition-colors text-sm"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {thisWeeksAppointments.length > 0 && (
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h2 className="text-2xl font-bold text-white mb-4">This Week's Appointments</h2>
          <p className="text-gray-400 mb-6">
            {thisWeeksAppointments.length} {thisWeeksAppointments.length === 1 ? "appointment" : "appointments"} scheduled for this week
          </p>

          <div className="space-y-3">
            {            thisWeeksAppointments.map((appointment) => {
              const { date, time } = formatAppointmentDateTime(appointment.appointmentDateTime);
              const patient = patientProfiles.get(appointment.patientId);
              const profilePicture = patient ? getImageUrl(patient.profilePicture) : null;
              const initials = patient 
                ? `${patient.firstName?.[0] || ""}${patient.lastName?.[0] || ""}`
                : appointment.patientName.split(' ').map(n => n[0]).join('').slice(0, 2);
              
              return (
                <div
                  key={appointment.id}
                  className="bg-gray-700/50 rounded-lg p-4 border border-gray-600 hover:border-gray-500 transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-4 flex-1">
                      <Avatar className="w-12 h-12 border-2 border-gray-600 flex-shrink-0">
                        <AvatarImage src={profilePicture || undefined} alt={appointment.patientName} />
                        <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-sm">
                          {initials}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex-1 min-w-0">
                        <button
                          onClick={() => handlePatientClick(appointment.patientId)}
                          disabled={loadingPatient}
                          className="text-left hover:text-blue-400 transition-colors w-full"
                        >
                          <h3 className="text-lg font-semibold text-white mb-1 hover:underline">
                            {appointment.patientName}
                          </h3>
                        </button>
                        {patient && (
                          <div className="text-xs text-gray-400 mb-2">
                            {patient.dateOfBirth && (
                              <span>DOB: {new Date(patient.dateOfBirth).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}</span>
                            )}
                            {patient.gender && (
                              <span className="ml-2">• {patient.gender}</span>
                            )}
                          </div>
                        )}
                        {appointment.notes && (
                          <p className="text-sm text-gray-300 mb-2">
                            <span className="text-gray-400">Reason: </span>
                            {appointment.notes}
                          </p>
                        )}
                        <div className="flex items-center space-x-4 text-sm text-gray-400">
                          <span>{date}</span>
                          <span>{time}</span>
                          <span className={`px-2 py-1 rounded text-xs ${getAppointmentStatusColor(getDisplayStatus(appointment))}`}>
                            {getDisplayStatus(appointment)}
                          </span>
                        </div>
                      </div>
                    </div>
                    {!isAppointmentPassed(appointment) && appointment.status !== "CANCELLED" && appointment.status !== "COMPLETED" && (
                      <button
                        onClick={() => handleCancel(appointment.id)}
                        className="ml-4 px-3 py-1 bg-red-600 text-white rounded hover:bg-red-700 transition-colors text-sm"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {sortedAppointments.length > todaysAppointments.length + thisWeeksAppointments.length && (
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h2 className="text-xl font-bold text-white mb-4">All Appointments</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sortedAppointments.map((appointment) => {
              const { date, time } = formatAppointmentDateTime(appointment.appointmentDateTime);
              return (
                <div key={appointment.id} className="card">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-start space-x-3 flex-1">
                      {(() => {
                        const patient = patientProfiles.get(appointment.patientId);
                        const profilePicture = patient ? getImageUrl(patient.profilePicture) : null;
                        const initials = patient 
                          ? `${patient.firstName?.[0] || ""}${patient.lastName?.[0] || ""}`
                          : appointment.patientName.split(' ').map(n => n[0]).join('').slice(0, 2);
                        
                        return (
                          <Avatar className="w-10 h-10 border-2 border-gray-600 flex-shrink-0">
                            <AvatarImage src={profilePicture || undefined} alt={appointment.patientName} />
                            <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-xs">
                              {initials}
                            </AvatarFallback>
                          </Avatar>
                        );
                      })()}
                      <div className="flex-1 min-w-0">
                        <button
                          onClick={() => handlePatientClick(appointment.patientId)}
                          disabled={loadingPatient}
                          className="text-left hover:text-blue-400 transition-colors w-full"
                        >
                          <h3 className="text-lg font-semibold text-white mb-1 hover:underline">
                            {appointment.patientName}
                          </h3>
                        </button>
                        {(() => {
                          const patient = patientProfiles.get(appointment.patientId);
                          if (patient) {
                            return (
                              <div className="text-xs text-gray-400">
                                {patient.dateOfBirth && (
                                  <span>DOB: {new Date(patient.dateOfBirth).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}</span>
                                )}
                                {patient.gender && (
                                  <span className="ml-2">• {patient.gender}</span>
                                )}
                              </div>
                            );
                          }
                          return null;
                        })()}
                      </div>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getAppointmentStatusColor(getDisplayStatus(appointment))}`}>
                      {getDisplayStatus(appointment)}
                    </span>
                  </div>

                  <div className="space-y-2 mb-4">
                    <div className="flex items-center text-gray-400">
                      <HiCalendar className="w-4 h-4 mr-2" />
                      <span className="text-sm">{date}</span>
                    </div>
                    <div className="flex items-center text-gray-400">
                      <HiClock className="w-4 h-4 mr-2" />
                      <span className="text-sm">{time}</span>
                    </div>
                  </div>

                  {appointment.notes && (
                    <div className="mb-4 p-3 bg-gray-700/50 rounded-lg">
                      <p className="text-xs text-gray-400 mb-1">Reason for visit:</p>
                      <p className="text-sm text-gray-300">{appointment.notes}</p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function PatientAppointmentsView({
  appointments,
  handleCancel,
}: {
  appointments: Appointment[];
  handleCancel: (id: number) => void;
}) {
  const sortedAppointments = sortAppointments(appointments);
  return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {sortedAppointments.map((appointment) => {
            const { date, time } = formatAppointmentDateTime(appointment.appointmentDateTime);
            return (
              <div key={appointment.id} className="card">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-white mb-2">
                  {appointment.doctorName}
                    </h3>
                {appointment.doctorSpecialization && (
                      <p className="text-sm text-gray-400 mb-2">
                        {appointment.doctorSpecialization}
                      </p>
                    )}
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getAppointmentStatusColor(getDisplayStatus(appointment))}`}>
                    {getDisplayStatus(appointment)}
                  </span>
                </div>

                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-gray-400">
                    <HiCalendar className="w-4 h-4 mr-2" />
                    <span className="text-sm">{date}</span>
                  </div>
                  <div className="flex items-center text-gray-400">
                    <HiClock className="w-4 h-4 mr-2" />
                    <span className="text-sm">{time}</span>
                  </div>
                </div>

                {appointment.notes && (
                  <div className="mb-4 p-3 bg-gray-700/50 rounded-lg">
                    <p className="text-sm text-gray-300">{appointment.notes}</p>
                  </div>
                )}

                {!isAppointmentPassed(appointment) && appointment.status !== "CANCELLED" && appointment.status !== "COMPLETED" && (
                  <div className="flex space-x-2">
                    <button
                      onClick={() => handleCancel(appointment.id)}
                      className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors flex items-center justify-center space-x-2"
                    >
                      <HiX className="w-4 h-4" />
                      <span>Cancel</span>
                    </button>
                  </div>
                )}
              </div>
            );
          })}
    </div>
  );
}


