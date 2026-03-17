
export const validators = {
  email: (email: string): string | null => {
    if (!email) return "Email is required";
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) return "Invalid email format";
    return null;
  },

  password: (password: string): string | null => {
    if (!password) return "Password is required";
    if (password.length < 7) return "Password must be at least 7 characters long";
    return null;
  },

  passwordMatch: (password: string, confirm: string): string | null => {
    if (password !== confirm) return "Passwords do not match";
    return null;
  },

  required: (value: string, fieldName: string): string | null => {
    if (!value || value.trim() === "") return `${fieldName} is required`;
    return null;
  },

  name: (name: string, fieldName: string = "Name"): string | null => {
    if (!name || name.trim() === "") return `${fieldName} is required`;
    if (name.trim().length < 2) return `${fieldName} must be at least 2 characters`;
    if (name.trim().length > 50) return `${fieldName} must be less than 50 characters`;
  
    const nameRegex = /^[a-zA-Z\s'-]+$/;
    if (!nameRegex.test(name.trim())) return `${fieldName} can only contain letters, spaces, hyphens, and apostrophes`;
    return null;
  },

  textLength: (text: string | undefined | null, fieldName: string, maxLength: number, minLength: number = 0): string | null => {
    if (text === undefined || text === null) return null; // Optional 
    if (minLength > 0 && text.trim().length < minLength) {
      return `${fieldName} must be at least ${minLength} characters`;
    }
    if (text.length > maxLength) {
      return `${fieldName} must be less than ${maxLength} characters`;
    }
    return null;
  },

  experience: (value: number | undefined | null): string | null => {
    if (value === undefined || value === null || Number.isNaN(value)) {
      return "Years of experience is required";
    }
    if (value < 0 || value > 50) {
      return "Years of experience must be between 0 and 50";
    }
    return null;
  },

  dateOfBirth: (date: string | undefined | null): string | null => {
    if (!date) return "Date of birth is required";
    const birthDate = new Date(date);
    const today = new Date();
    const maxAge = 120;
    const minDate = new Date();
    minDate.setFullYear(today.getFullYear() - maxAge);
    
    if (isNaN(birthDate.getTime())) {
      return "Please enter a valid date";
    }
    if (birthDate >= today) {
      return "Date of birth must be in the past";
    }
    if (birthDate < minDate) {
      return "Please enter a valid date of birth";
    }
    return null;
  },

  licenseNumber: (license: string | undefined | null): string | null => {
    if (!license || license.trim() === "") return "License number is required";
    if (license.trim().length < 3) return "License number must be at least 3 characters";
    if (license.trim().length > 50) return "License number must be less than 50 characters";
  
    const licenseRegex = /^[0-9]+$/;
    if (!licenseRegex.test(license.trim())) {
      return "License number can only contain numbers";
    }
    return null;
  },

  clinicName: (clinic: string | undefined | null): string | null => {
    if (!clinic || clinic.trim() === "") return "Clinic name is required";
    if (clinic.trim().length < 2) return "Clinic name must be at least 2 characters";
    if (clinic.trim().length > 100) return "Clinic name must be less than 100 characters";
    return null;
  },

  specialization: (specialization: string | undefined | null): string | null => {
    if (!specialization || specialization.trim() === "") return "Specialization is required";
    if (specialization.trim().length < 2) return "Specialization must be at least 2 characters";
    if (specialization.trim().length > 100) return "Specialization must be less than 100 characters";
    return null;
  },

  insuranceNumber: (insurance: string | undefined | null): string | null => {
    // Optional field, but if provided, validate it
    if (!insurance || insurance.trim() === "") return null;
    if (insurance.trim().length < 3) return "Insurance number must be at least 3 characters";
    if (insurance.trim().length > 50) return "Insurance number must be less than 50 characters";
    const insuranceRegex = /^[0-9]+$/;
    if (!insuranceRegex.test(insurance.trim())) {
      return "Insurance number can only contain numbers";
    }
    return null;
  },
};

