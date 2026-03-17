import axios from "axios";

const API = axios.create({
  baseURL: "/api",
});

API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers = config.headers ?? {};
    (config.headers as any).Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 errors globally - clear token and redirect to login
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear authentication data
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      // Redirect to login page
      if (window.location.pathname !== "/login" && window.location.pathname !== "/register") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export type LoginRequest = { email: string; password: string };
export type RegisterRequest = { email: string; password: string; role: "DOCTOR" | "PATIENT" };
export type JwtResponse = { token: string; email: string; role: "DOCTOR" | "PATIENT"; onboardingRequired: boolean };
export type ForgotPasswordRequest = { email: string };
export type ResetPasswordRequest = { token: string; newPassword: string };

export type DoctorOnboardRequest = {
  firstName: string;
  lastName: string;
  specialization: string;
  bio?: string;
  licenseNumber: string;
  clinicName: string;
  yearsOfExperience: number;
  profilePhotoUrl?: string;
  openingHours?: string;
  closingHours?: string;
  isAbsent?: boolean;
};

export type PatientOnboardRequest = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender?: string;
  conditions?: string;
  insuranceNumber?: string;
  profilePicture?: string;
};

export type DoctorProfile = {
  id: number;
  firstName: string;
  lastName: string;
  specialization?: string;
  bio?: string;
  licenseNumber?: string;
  clinicName?: string;
  yearsOfExperience?: number;
  profilePhotoUrl?: string;
  openingHours?: string;
  closingHours?: string;
  isAbsent?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type PatientProfile = {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender?: string;
  conditions?: string;
  insuranceNumber?: string;
  profilePicture?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type AppointmentStatus = "SCHEDULED" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "PASSED";

export type CreateAppointmentRequest = {
  doctorId: number;
  appointmentDateTime: string; // ISO datetime string
  notes?: string;
};

export type UpdateAppointmentRequest = {
  appointmentDateTime?: string;
  status?: AppointmentStatus;
  notes?: string;
};

export type Appointment = {
  id: number;
  doctorId: number;
  doctorName: string;
  doctorSpecialization?: string;
  patientId: number;
  patientName: string;
  appointmentDateTime: string;
  status: AppointmentStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
};

export const AuthApi = {
  async login(body: LoginRequest) {
    const res = await API.post<JwtResponse>("/auth/login", body);
    return res.data;
  },
  async register(body: RegisterRequest) {
    await API.post("/auth/register", body);
  },
  async forgotPassword(body: ForgotPasswordRequest) {
    await API.post("/auth/forgot-password", body);
  },
  async resetPassword(body: ResetPasswordRequest) {
    await API.post("/auth/reset-password", body);
  },
};

export const DoctorApi = {
  async createOrUpdateProfile(body: DoctorOnboardRequest) {
    await API.post("/onboarding/doctor", body);
  },
  async updateProfile(body: DoctorOnboardRequest) {
    const res = await API.post<DoctorProfile>("/doctors/profile", body);
    return res.data;
  },
  async getMyProfile() {
    const res = await API.get<DoctorProfile>("/doctors/profile/me");
    return res.data;
  },
  async getAllDoctors() {
    const res = await API.get<DoctorProfile[]>("/doctors");
    return res.data;
  },
};

export type PatientSummary = {
  profileId: number;
  firstName: string;
  lastName: string;
  email: string;
};

export const PatientApi = {
  async getAllPatients() {
    const res = await API.get<PatientSummary[]>("/patients/all");
    return res.data;
  },
  async createOrUpdateProfile(body: PatientOnboardRequest) {
    await API.post("/onboarding/patient", body);
  },
  async updateProfile(body: PatientOnboardRequest) {
    const res = await API.post<PatientProfile>("/patients/profile", body);
    return res.data;
  },
  async getMyProfile() {
    const res = await API.get<PatientProfile>("/patients/profile/me");
    return res.data;
  },
  async getProfileById(id: number) {
    const res = await API.get<PatientProfile>(`/patients/profile/${id}`);
    return res.data;
  },
  async getPatientEmailByProfileId(id: number) {
    const res = await API.get<{ profileId: number; name: string; email: string }>(`/patients/profile/${id}/email`);
    return res.data;
  },
};

export const FileApi = {
  async uploadFile(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    
    const res = await API.post<{message: string; filePath: string; fileName: string}>("/files/upload", formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return res.data;
  },
  
  getFileUrl(fileName: string | undefined | null) {
    if (!fileName) return null;
    // If it's already a full URL, return it
    if (fileName.startsWith('http://') || fileName.startsWith('https://') || fileName.startsWith('/')) {
      // If it starts with /api/files, return as is
      if (fileName.startsWith('/api/files/')) {
        return fileName;
      }
      // If it's just a filename, construct the URL
      if (!fileName.includes('/')) {
        return `/api/files/${fileName}`;
      }
      // Otherwise return as is (might be a relative path)
      return fileName;
    }
    // Just a filename, construct the URL
    return `/api/files/${fileName}`;
  }
};

export const OnboardingApi = {
  async updateRole(role: "DOCTOR" | "PATIENT") {
    await API.post("/onboarding/role", { role });
  },
  async getSummary() {
    const res = await API.get("/onboarding/summary");
    return res.data;
  },
};

export type AvailableTimeSlot = {
  time: string;
  available: boolean;
};

export const AppointmentApi = {
  async createAppointment(body: CreateAppointmentRequest) {
    const res = await API.post<Appointment>("/appointments", body);
    return res.data;
  },
  async getMyAppointments() {
    const res = await API.get<Appointment[]>("/appointments/me");
    return res.data;
  },
  async getAppointment(id: number) {
    const res = await API.get<Appointment>(`/appointments/${id}`);
    return res.data;
  },
  async updateAppointment(id: number, body: UpdateAppointmentRequest) {
    const res = await API.put<Appointment>(`/appointments/${id}`, body);
    return res.data;
  },
  async cancelAppointment(id: number) {
    await API.delete(`/appointments/${id}`);
  },
  async getAvailableTimeSlots(doctorId: number, date: string) {
    const res = await API.get<AvailableTimeSlot[]>(`/appointments/availability/${doctorId}?date=${date}`);
    return res.data;
  },
};

export type ChatMessage = {
  role: "user" | "assistant";
  content: string;
};

export type ChatRequest = {
  message: string;
  conversationHistory?: ChatMessage[];
};

export type ChatResponse = {
  response: string;
  conversationHistory: ChatMessage[];
};

export const ChatApi = {
  async sendMessage(body: ChatRequest) {
    const res = await API.post<ChatResponse>("/chat", body);
    return res.data;
  },
};

export type PrescriptionStatus = "DRAFT" | "ACTIVE" | "DISCONTINUED" | "EXPIRED";

export type Prescription = {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions?: string;
  status: PrescriptionStatus;
  issuedAt: string;
  validUntil: string;
  createdAt: string;
  updatedAt: string;
};

export type CreatePrescriptionRequest = {
  patientId: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions?: string;
  validUntil: string;
};

export type UpdatePrescriptionRequest = {
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions?: string;
  validUntil: string;
};

export const PrescriptionApi = {
  async createPrescription(body: CreatePrescriptionRequest) {
    const res = await API.post<Prescription>("/prescriptions", body);
    return res.data;
  },
  async updatePrescription(id: number, body: UpdatePrescriptionRequest) {
    const res = await API.put<Prescription>(`/prescriptions/${id}`, body);
    return res.data;
  },
  async activatePrescription(id: number) {
    const res = await API.post<Prescription>(`/prescriptions/${id}/activate`);
    return res.data;
  },
  async discontinuePrescription(id: number) {
    const res = await API.post<Prescription>(`/prescriptions/${id}/discontinue`);
    return res.data;
  },
  async getMyPrescriptions() {
    const res = await API.get<Prescription[]>("/prescriptions/doctor");
    return res.data;
  },
  async getPatientPrescriptions() {
    const res = await API.get<Prescription[]>("/prescriptions/patient");
    return res.data;
  },
  async getPrescription(id: number) {
    const res = await API.get<Prescription>(`/prescriptions/${id}`);
    return res.data;
  },
};

export type UserInfo = {
  userId: number;
  email: string;
  role: string;
  firstName: string;
  lastName: string;
  profileId: number | null;
};

export const UserApi = {
  async getAllUsers() {
    const res = await API.get<UserInfo[]>("/users");
    return res.data;
  },
};

export default API;
