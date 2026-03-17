import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { DoctorApi, AppointmentApi, DoctorProfile, CreateAppointmentRequest, AvailableTimeSlot } from "../api";
import { HiArrowLeft, HiArrowRight, HiXCircle } from "react-icons/hi";
import SpecialtyFilter from "../components/appointment/SpecialtyFilter";
import DoctorSearchBar from "../components/appointment/DoctorSearchBar";
import DoctorCard from "../components/appointment/DoctorCard";
import WeekCalendarWithSlots from "../components/appointment/WeekCalendarWithSlots";
import BookingSteps from "../components/appointment/BookingSteps";
import { useDoctorFilter } from "../hooks/useDoctorFilter";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import Select from "@/components/ui/select";
import { getImageUrl } from "../utils/imageUtils";

const STEPS = ["Select Doctor", "Choose Date & Time", "Review & Confirm"];

export default function BookAppointment() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [doctors, setDoctors] = useState<DoctorProfile[]>([]);
  const [selectedDoctor, setSelectedDoctor] = useState<DoctorProfile | null>(null);
  const [selectedSpecialty, setSelectedSpecialty] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [availableSlots, setAvailableSlots] = useState<AvailableTimeSlot[]>([]);
  const [appointmentReason, setAppointmentReason] = useState<string>("");
  const [customReason, setCustomReason] = useState<string>("");
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { filteredDoctors } = useDoctorFilter({
    doctors,
    searchQuery,
    selectedSpecialty,
  });

  useEffect(() => {
    fetchDoctors();
  }, []);

  useEffect(() => {
    if (selectedDoctor && selectedDate && currentStep >= 2) {
      fetchAvailableSlots();
    }
  }, [selectedDoctor, selectedDate, currentStep]);

  const fetchDoctors = async () => {
    try {
      setLoading(true);
      const data = await DoctorApi.getAllDoctors();
      setDoctors(data);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load doctors");
    } finally {
      setLoading(false);
    }
  };

  const fetchAvailableSlots = async () => {
    if (!selectedDoctor || !selectedDate) return;

    try {
      setLoadingSlots(true);
      const dateString = selectedDate.toISOString().split("T")[0];
      const slots = await AppointmentApi.getAvailableTimeSlots(selectedDoctor.id!, dateString);
      setAvailableSlots(slots);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load available time slots");
    } finally {
      setLoadingSlots(false);
    }
  };

  const handleDoctorSelect = (doctor: DoctorProfile) => {
    setSelectedDoctor(doctor);
    setError(null);
  };

  const handleDateSelect = (date: Date) => {
    setSelectedDate(date);
    setSelectedTime(null);
    setError(null);
  };

  const handleTimeSelect = (time: string) => {
    setSelectedTime(time);
    setError(null);
  };

  const handleNext = () => {
    if (currentStep === 1 && !selectedDoctor) {
      setError("Please select a doctor to continue");
      return;
    }
    if (currentStep === 2 && (!selectedDate || !selectedTime)) {
      setError("Please select a date and time to continue");
      return;
    }
    setError(null);
    setCurrentStep(currentStep + 1);
  };

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
      setError(null);
    }
  };

  const handleSubmit = async () => {
    if (!selectedDoctor || !selectedDate || !selectedTime) {
      setError("Please complete all required fields");
      return;
    }

    if (!appointmentReason) {
      setError("Please select a reason for the appointment");
      return;
    }

    if (appointmentReason === "OTHER" && !customReason.trim()) {
      setError("Please specify the reason for your appointment");
      return;
    }

    // Construct the datetime string in local time without timezone conversion
    // This preserves the exact time selected without timezone shifts
    const year = selectedDate.getFullYear();
    const month = String(selectedDate.getMonth() + 1).padStart(2, '0');
    const day = String(selectedDate.getDate()).padStart(2, '0');
    const appointmentDateTime = `${year}-${month}-${day}T${selectedTime}:00`;

    setSubmitting(true);
    setError(null);

    try {
      // Build notes string with reason
      let notesText = notes.trim();
      if (appointmentReason) {
        const reasonText = `Reason: ${formatReason(appointmentReason)}`;
        notesText = notesText ? `${reasonText}\n\n${notesText}` : reasonText;
      }

      const request: CreateAppointmentRequest = {
        doctorId: selectedDoctor.id!,
        appointmentDateTime,
        notes: notesText || undefined,
      };

      await AppointmentApi.createAppointment(request);
      navigate("/appointments", { state: { success: "Appointment booked successfully!" } });
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to book appointment");
    } finally {
      setSubmitting(false);
    }
  };

  const formatDate = (date: Date | null): string => {
    if (!date) return "";
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const formatReason = (reason: string): string => {
    if (!reason) return "";
    if (reason === "OTHER") return customReason.trim() || "Other";
    return reason
      .split("_")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(" ");
  };

  if (loading) {
    return (
      <div className="p-6 flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen p-8">
      <div className="max-w-[1600px] mx-auto">
        <div className="w-full">
          <h1 className="text-5xl font-bold text-white mb-3">Book Appointment</h1>
          <p className="text-gray-400 mb-8 text-xl">Follow the steps to book your appointment</p>

          <BookingSteps currentStep={currentStep} steps={STEPS} />

          {error && (
            <div className="mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400">
              {error}
            </div>
          )}

          {currentStep === 1 && (
            <div className="space-y-6">
              <DoctorSearchBar
                searchQuery={searchQuery}
                onSearchChange={setSearchQuery}
              />

              <SpecialtyFilter
                selectedSpecialty={selectedSpecialty}
                onSpecialtyChange={setSelectedSpecialty}
              />

              <div>
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-semibold text-white">Choose doctor</h2>
                  {filteredDoctors.length > 0 && (
                    <span className="text-sm text-gray-400">
                      {filteredDoctors.length} {filteredDoctors.length === 1 ? "doctor" : "doctors"} found
                    </span>
                  )}
                </div>
                {filteredDoctors.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
                    {filteredDoctors.map((doctor) => (
                      <DoctorCard
                        key={doctor.id}
                        doctor={doctor}
                        isSelected={selectedDoctor?.id === doctor.id}
                        onSelect={handleDoctorSelect}
                      />
                    ))}
                  </div>
                ) : (
                  <div className="p-12 text-center bg-gray-800/50 rounded-xl border border-gray-700">
                    <p className="text-gray-400 text-base mb-4">
                      {doctors.length === 0
                        ? "No doctors available"
                        : "No doctors found matching your search criteria"}
                    </p>
                    {(searchQuery || selectedSpecialty) && (
                      <Button
                        type="button"
                        variant="link"
                        onClick={() => {
                          setSearchQuery("");
                          setSelectedSpecialty(null);
                        }}
                        className="text-sm"
                      >
                        Clear filters
                      </Button>
                    )}
                  </div>
                )}
              </div>

              <div className="flex justify-end pt-4">
                <Button
                  onClick={handleNext}
                  disabled={!selectedDoctor}
                  size="lg"
                  className="shadow-lg shadow-blue-500/30"
                >
                  <span>Next</span>
                  <HiArrowRight className="w-5 h-5" />
                </Button>
              </div>
            </div>
          )}

              {currentStep === 2 && (
                <div className="space-y-6">
                  {selectedDoctor && (
                    <>
                      {selectedDoctor.isAbsent && (
                        <div className="mb-6 p-4 bg-yellow-500/20 border border-yellow-500/50 rounded-lg flex items-center gap-3">
                          <HiXCircle className="w-6 h-6 text-yellow-400 flex-shrink-0" />
                          <div>
                            <p className="text-yellow-400 font-semibold mb-1">
                              Dr. {selectedDoctor.firstName} {selectedDoctor.lastName} is currently out of office
                            </p>
                            <p className="text-yellow-300 text-sm">
                              All time slots are currently unavailable. Please try again later or select another doctor.
                            </p>
                          </div>
                        </div>
                      )}
                      <Card className="mb-6 border-gray-700 bg-gray-800">
                        <CardContent className="p-4">
                          <p className="text-sm text-gray-400 mb-2">Selected Doctor</p>
                          <div className="flex items-center space-x-4">
                            <Avatar className="w-16 h-16 border-2 border-gray-700 flex-shrink-0">
                              <AvatarImage src={getImageUrl(selectedDoctor.profilePhotoUrl) || undefined} />
                              <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-lg">
                                {selectedDoctor.firstName?.[0] || ""}{selectedDoctor.lastName?.[0] || ""}
                              </AvatarFallback>
                            </Avatar>
                            <div>
                              <p className="text-lg font-semibold text-white">
                                Dr. {selectedDoctor.firstName} {selectedDoctor.lastName}
                              </p>
                              {selectedDoctor.specialization && (
                                <p className="text-sm text-gray-400">{selectedDoctor.specialization}</p>
                              )}
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    </>
                  )}

              <WeekCalendarWithSlots
                selectedDate={selectedDate}
                selectedTime={selectedTime}
                onDateSelect={handleDateSelect}
                onTimeSelect={handleTimeSelect}
                availableSlots={availableSlots}
                loading={loadingSlots}
              />

              <div className="flex justify-between pt-4">
                <button
                  onClick={handleBack}
                  className="px-8 py-4 bg-gray-800 text-white rounded-xl hover:bg-gray-700 transition-colors font-medium border border-gray-700 flex items-center space-x-2"
                >
                  <HiArrowLeft className="w-5 h-5" />
                  <span>Back</span>
                </button>
                <button
                  onClick={handleNext}
                  disabled={!selectedDate || !selectedTime}
                  className="px-8 py-4 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium shadow-lg shadow-blue-500/30 flex items-center space-x-2"
                >
                  <span>Next</span>
                  <HiArrowRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          )}

          {currentStep === 3 && (
            <div className="space-y-6">
              <div className="bg-gray-800 rounded-xl p-6 border border-gray-700 space-y-4">
                <h2 className="text-2xl font-semibold text-white mb-4">Review Your Appointment</h2>

                <div className="space-y-3">
                  <div>
                    <p className="text-sm text-gray-400 mb-2">Doctor</p>
                    <div className="flex items-center space-x-4">
                      <Avatar className="w-16 h-16 border-2 border-gray-700 flex-shrink-0">
                        <AvatarImage src={selectedDoctor && getImageUrl(selectedDoctor.profilePhotoUrl) || undefined} />
                        <AvatarFallback className="bg-gradient-to-br from-blue-500 to-cyan-400 text-white font-bold text-lg">
                          {selectedDoctor?.firstName?.[0] || ""}{selectedDoctor?.lastName?.[0] || ""}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="text-lg font-semibold text-white">
                          Dr. {selectedDoctor?.firstName} {selectedDoctor?.lastName}
                        </p>
                        {selectedDoctor?.specialization && (
                          <p className="text-sm text-gray-400">{selectedDoctor.specialization}</p>
                        )}
                      </div>
                    </div>
                  </div>

                  <div>
                    <p className="text-sm text-gray-400 mb-1">Date</p>
                    <p className="text-lg font-semibold text-white">{formatDate(selectedDate)}</p>
                  </div>

                  <div>
                    <p className="text-sm text-gray-400 mb-1">Time</p>
                    <p className="text-lg font-semibold text-white">{selectedTime}</p>
                  </div>

                  {appointmentReason && (
                    <div>
                      <p className="text-sm text-gray-400 mb-1">Reason</p>
                      <p className="text-lg font-semibold text-white">
                        {formatReason(appointmentReason)}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              <div className="space-y-6">
                <div>
                  <Label htmlFor="appointmentReason" className="text-base font-medium text-gray-300 mb-3 block">
                    Reason for Appointment <span className="text-red-400">*</span>
                  </Label>
                  <Select
                    id="appointmentReason"
                    value={appointmentReason}
                    onChange={(e) => {
                      setAppointmentReason(e.target.value);
                      if (e.target.value !== "OTHER") {
                        setCustomReason("");
                      }
                      setError(null);
                    }}
                    required
                    className="w-full bg-gray-800 border-gray-700 text-white focus:ring-blue-500/20"
                  >
                    <option value="">Select a reason...</option>
                    <option value="GENERAL_CHECKUP">General Checkup</option>
                    <option value="FOLLOW_UP">Follow-up Visit</option>
                    <option value="CONSULTATION">Consultation</option>
                    <option value="PRESCRIPTION_REFILL">Prescription Refill</option>
                    <option value="TEST_RESULTS">Test Results Review</option>
                    <option value="VACCINATION">Vaccination</option>
                    <option value="URGENT_CARE">Urgent Care</option>
                    <option value="OTHER">Other</option>
                  </Select>
                </div>

                {appointmentReason === "OTHER" && (
                  <div className="animate-in fade-in duration-300">
                    <Label htmlFor="customReason" className="text-base font-medium text-gray-300 mb-3 block">
                      Please specify <span className="text-red-400">*</span>
                    </Label>
                    <Textarea
                      id="customReason"
                      value={customReason}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                        setCustomReason(e.target.value);
                        setError(null);
                      }}
                      rows={3}
                      maxLength={200}
                      placeholder="Please describe the reason for your appointment..."
                      required
                      className="bg-gray-800 border-gray-700 focus:ring-blue-500/20"
                    />
                    <p className="text-xs text-gray-500 mt-2">{customReason.length}/200 characters</p>
                  </div>
                )}

                <div>
                  <Label htmlFor="notes" className="text-base font-medium text-gray-300 mb-3 block">
                    Additional Notes (Optional)
                  </Label>
                  <Textarea
                    id="notes"
                    value={notes}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setNotes(e.target.value)}
                    rows={5}
                    maxLength={1000}
                    placeholder="Any additional information or concerns..."
                    className="bg-gray-800 border-gray-700 focus:ring-blue-500/20"
                  />
                  <p className="text-xs text-gray-500 mt-2">{notes.length}/1000 characters</p>
                </div>
              </div>

              <div className="flex justify-between pt-4">
                <Button
                  onClick={handleBack}
                  variant="outline"
                  size="lg"
                  className="border-gray-700"
                >
                  <HiArrowLeft className="w-5 h-5" />
                  <span>Back</span>
                </Button>
                <Button
                  onClick={handleSubmit}
                  disabled={submitting}
                  size="lg"
                  className="shadow-lg shadow-blue-500/30"
                >
                  {submitting ? "Booking..." : "Confirm & Book Appointment"}
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
