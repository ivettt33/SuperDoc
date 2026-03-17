import { useMemo } from "react";
import { DoctorProfile } from "../api";

interface UseDoctorFilterProps {
  doctors: DoctorProfile[];
  searchQuery: string;
  selectedSpecialty: string | null;
}

export function useDoctorFilter({ doctors, searchQuery, selectedSpecialty }: UseDoctorFilterProps) {
  const filteredDoctors = useMemo(() => {
    return doctors.filter((doctor) => {
      if (selectedSpecialty && doctor.specialization !== selectedSpecialty) {
        return false;
      }

      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim();
        const fullName = `${doctor.firstName} ${doctor.lastName}`.toLowerCase();
        const specialization = doctor.specialization?.toLowerCase() || "";
        const clinicName = doctor.clinicName?.toLowerCase() || "";

        return (
          fullName.includes(query) ||
          specialization.includes(query) ||
          clinicName.includes(query)
        );
      }

      return true;
    });
  }, [doctors, searchQuery, selectedSpecialty]);

  return { filteredDoctors };
}

