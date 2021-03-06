// Code generated by protoc-gen-go. DO NOT EDIT.
// source: auth.proto

package auth

import (
	fmt "fmt"
	proto "github.com/golang/protobuf/proto"
	math "math"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion3 // please upgrade the proto package

// Types of authentication token
type Flavor int32

const (
	Flavor_AUTH_NONE Flavor = 0
	Flavor_AUTH_SYS  Flavor = 1
)

var Flavor_name = map[int32]string{
	0: "AUTH_NONE",
	1: "AUTH_SYS",
}

var Flavor_value = map[string]int32{
	"AUTH_NONE": 0,
	"AUTH_SYS":  1,
}

func (x Flavor) String() string {
	return proto.EnumName(Flavor_name, int32(x))
}

func (Flavor) EnumDescriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{0}
}

type Token struct {
	Flavor               Flavor   `protobuf:"varint,1,opt,name=flavor,proto3,enum=auth.Flavor" json:"flavor,omitempty"`
	Data                 []byte   `protobuf:"bytes,2,opt,name=data,proto3" json:"data,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Token) Reset()         { *m = Token{} }
func (m *Token) String() string { return proto.CompactTextString(m) }
func (*Token) ProtoMessage()    {}
func (*Token) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{0}
}

func (m *Token) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Token.Unmarshal(m, b)
}
func (m *Token) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Token.Marshal(b, m, deterministic)
}
func (m *Token) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Token.Merge(m, src)
}
func (m *Token) XXX_Size() int {
	return xxx_messageInfo_Token.Size(m)
}
func (m *Token) XXX_DiscardUnknown() {
	xxx_messageInfo_Token.DiscardUnknown(m)
}

var xxx_messageInfo_Token proto.InternalMessageInfo

func (m *Token) GetFlavor() Flavor {
	if m != nil {
		return m.Flavor
	}
	return Flavor_AUTH_NONE
}

func (m *Token) GetData() []byte {
	if m != nil {
		return m.Data
	}
	return nil
}

// Token structure for AUTH_SYS flavor cred
type Sys struct {
	Stamp                uint64   `protobuf:"varint,1,opt,name=stamp,proto3" json:"stamp,omitempty"`
	Machinename          string   `protobuf:"bytes,2,opt,name=machinename,proto3" json:"machinename,omitempty"`
	User                 string   `protobuf:"bytes,3,opt,name=user,proto3" json:"user,omitempty"`
	Group                string   `protobuf:"bytes,4,opt,name=group,proto3" json:"group,omitempty"`
	Groups               []string `protobuf:"bytes,5,rep,name=groups,proto3" json:"groups,omitempty"`
	Secctx               string   `protobuf:"bytes,6,opt,name=secctx,proto3" json:"secctx,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Sys) Reset()         { *m = Sys{} }
func (m *Sys) String() string { return proto.CompactTextString(m) }
func (*Sys) ProtoMessage()    {}
func (*Sys) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{1}
}

func (m *Sys) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Sys.Unmarshal(m, b)
}
func (m *Sys) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Sys.Marshal(b, m, deterministic)
}
func (m *Sys) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Sys.Merge(m, src)
}
func (m *Sys) XXX_Size() int {
	return xxx_messageInfo_Sys.Size(m)
}
func (m *Sys) XXX_DiscardUnknown() {
	xxx_messageInfo_Sys.DiscardUnknown(m)
}

var xxx_messageInfo_Sys proto.InternalMessageInfo

func (m *Sys) GetStamp() uint64 {
	if m != nil {
		return m.Stamp
	}
	return 0
}

func (m *Sys) GetMachinename() string {
	if m != nil {
		return m.Machinename
	}
	return ""
}

func (m *Sys) GetUser() string {
	if m != nil {
		return m.User
	}
	return ""
}

func (m *Sys) GetGroup() string {
	if m != nil {
		return m.Group
	}
	return ""
}

func (m *Sys) GetGroups() []string {
	if m != nil {
		return m.Groups
	}
	return nil
}

func (m *Sys) GetSecctx() string {
	if m != nil {
		return m.Secctx
	}
	return ""
}

// Token and verifier are expected to have the same flavor type.
type Credential struct {
	Token                *Token   `protobuf:"bytes,1,opt,name=token,proto3" json:"token,omitempty"`
	Verifier             *Token   `protobuf:"bytes,2,opt,name=verifier,proto3" json:"verifier,omitempty"`
	Origin               string   `protobuf:"bytes,3,opt,name=origin,proto3" json:"origin,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Credential) Reset()         { *m = Credential{} }
func (m *Credential) String() string { return proto.CompactTextString(m) }
func (*Credential) ProtoMessage()    {}
func (*Credential) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{2}
}

func (m *Credential) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Credential.Unmarshal(m, b)
}
func (m *Credential) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Credential.Marshal(b, m, deterministic)
}
func (m *Credential) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Credential.Merge(m, src)
}
func (m *Credential) XXX_Size() int {
	return xxx_messageInfo_Credential.Size(m)
}
func (m *Credential) XXX_DiscardUnknown() {
	xxx_messageInfo_Credential.DiscardUnknown(m)
}

var xxx_messageInfo_Credential proto.InternalMessageInfo

func (m *Credential) GetToken() *Token {
	if m != nil {
		return m.Token
	}
	return nil
}

func (m *Credential) GetVerifier() *Token {
	if m != nil {
		return m.Verifier
	}
	return nil
}

func (m *Credential) GetOrigin() string {
	if m != nil {
		return m.Origin
	}
	return ""
}

// GetCredResp represents the result of a request to fetch authentication
// credentials.
type GetCredResp struct {
	Status               int32       `protobuf:"varint,1,opt,name=status,proto3" json:"status,omitempty"`
	Cred                 *Credential `protobuf:"bytes,2,opt,name=cred,proto3" json:"cred,omitempty"`
	XXX_NoUnkeyedLiteral struct{}    `json:"-"`
	XXX_unrecognized     []byte      `json:"-"`
	XXX_sizecache        int32       `json:"-"`
}

func (m *GetCredResp) Reset()         { *m = GetCredResp{} }
func (m *GetCredResp) String() string { return proto.CompactTextString(m) }
func (*GetCredResp) ProtoMessage()    {}
func (*GetCredResp) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{3}
}

func (m *GetCredResp) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_GetCredResp.Unmarshal(m, b)
}
func (m *GetCredResp) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_GetCredResp.Marshal(b, m, deterministic)
}
func (m *GetCredResp) XXX_Merge(src proto.Message) {
	xxx_messageInfo_GetCredResp.Merge(m, src)
}
func (m *GetCredResp) XXX_Size() int {
	return xxx_messageInfo_GetCredResp.Size(m)
}
func (m *GetCredResp) XXX_DiscardUnknown() {
	xxx_messageInfo_GetCredResp.DiscardUnknown(m)
}

var xxx_messageInfo_GetCredResp proto.InternalMessageInfo

func (m *GetCredResp) GetStatus() int32 {
	if m != nil {
		return m.Status
	}
	return 0
}

func (m *GetCredResp) GetCred() *Credential {
	if m != nil {
		return m.Cred
	}
	return nil
}

// ValidateCredReq represents a request to verify a set of authentication
// credentials.
type ValidateCredReq struct {
	Cred                 *Credential `protobuf:"bytes,1,opt,name=cred,proto3" json:"cred,omitempty"`
	XXX_NoUnkeyedLiteral struct{}    `json:"-"`
	XXX_unrecognized     []byte      `json:"-"`
	XXX_sizecache        int32       `json:"-"`
}

func (m *ValidateCredReq) Reset()         { *m = ValidateCredReq{} }
func (m *ValidateCredReq) String() string { return proto.CompactTextString(m) }
func (*ValidateCredReq) ProtoMessage()    {}
func (*ValidateCredReq) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{4}
}

func (m *ValidateCredReq) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_ValidateCredReq.Unmarshal(m, b)
}
func (m *ValidateCredReq) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_ValidateCredReq.Marshal(b, m, deterministic)
}
func (m *ValidateCredReq) XXX_Merge(src proto.Message) {
	xxx_messageInfo_ValidateCredReq.Merge(m, src)
}
func (m *ValidateCredReq) XXX_Size() int {
	return xxx_messageInfo_ValidateCredReq.Size(m)
}
func (m *ValidateCredReq) XXX_DiscardUnknown() {
	xxx_messageInfo_ValidateCredReq.DiscardUnknown(m)
}

var xxx_messageInfo_ValidateCredReq proto.InternalMessageInfo

func (m *ValidateCredReq) GetCred() *Credential {
	if m != nil {
		return m.Cred
	}
	return nil
}

// ValidateCredResp represents the result of a request to validate
// authentication credentials.
type ValidateCredResp struct {
	Status               int32    `protobuf:"varint,1,opt,name=status,proto3" json:"status,omitempty"`
	Token                *Token   `protobuf:"bytes,2,opt,name=token,proto3" json:"token,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *ValidateCredResp) Reset()         { *m = ValidateCredResp{} }
func (m *ValidateCredResp) String() string { return proto.CompactTextString(m) }
func (*ValidateCredResp) ProtoMessage()    {}
func (*ValidateCredResp) Descriptor() ([]byte, []int) {
	return fileDescriptor_8bbd6f3875b0e874, []int{5}
}

func (m *ValidateCredResp) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_ValidateCredResp.Unmarshal(m, b)
}
func (m *ValidateCredResp) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_ValidateCredResp.Marshal(b, m, deterministic)
}
func (m *ValidateCredResp) XXX_Merge(src proto.Message) {
	xxx_messageInfo_ValidateCredResp.Merge(m, src)
}
func (m *ValidateCredResp) XXX_Size() int {
	return xxx_messageInfo_ValidateCredResp.Size(m)
}
func (m *ValidateCredResp) XXX_DiscardUnknown() {
	xxx_messageInfo_ValidateCredResp.DiscardUnknown(m)
}

var xxx_messageInfo_ValidateCredResp proto.InternalMessageInfo

func (m *ValidateCredResp) GetStatus() int32 {
	if m != nil {
		return m.Status
	}
	return 0
}

func (m *ValidateCredResp) GetToken() *Token {
	if m != nil {
		return m.Token
	}
	return nil
}

func init() {
	proto.RegisterEnum("auth.Flavor", Flavor_name, Flavor_value)
	proto.RegisterType((*Token)(nil), "auth.Token")
	proto.RegisterType((*Sys)(nil), "auth.Sys")
	proto.RegisterType((*Credential)(nil), "auth.Credential")
	proto.RegisterType((*GetCredResp)(nil), "auth.GetCredResp")
	proto.RegisterType((*ValidateCredReq)(nil), "auth.ValidateCredReq")
	proto.RegisterType((*ValidateCredResp)(nil), "auth.ValidateCredResp")
}

func init() {
	proto.RegisterFile("auth.proto", fileDescriptor_8bbd6f3875b0e874)
}

var fileDescriptor_8bbd6f3875b0e874 = []byte{
	// 392 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0x7c, 0x52, 0xdd, 0xca, 0xd3, 0x40,
	0x10, 0x35, 0x5f, 0x93, 0xd0, 0x4e, 0xaa, 0x86, 0x45, 0x24, 0x97, 0x31, 0x54, 0x2c, 0x82, 0x0d,
	0xd4, 0x0b, 0x91, 0x5e, 0x55, 0xf1, 0x07, 0xc4, 0x0a, 0xdb, 0x2a, 0xe8, 0x8d, 0x6c, 0x37, 0xdb,
	0x76, 0x69, 0x92, 0x8d, 0xbb, 0x9b, 0x62, 0x9f, 0xc4, 0xd7, 0x95, 0x9d, 0x84, 0xfa, 0x03, 0x5f,
	0x6f, 0xc2, 0x39, 0x73, 0x66, 0xe6, 0x9c, 0x09, 0x0b, 0xc0, 0x5a, 0x7b, 0x98, 0x35, 0x5a, 0x59,
	0x45, 0x7c, 0x87, 0xb3, 0x25, 0x04, 0x1b, 0x75, 0x14, 0x35, 0x99, 0x40, 0xb8, 0x2b, 0xd9, 0x49,
	0xe9, 0xc4, 0x4b, 0xbd, 0xe9, 0xbd, 0xf9, 0x78, 0x86, 0xbd, 0x6f, 0xb1, 0x46, 0x7b, 0x8d, 0x10,
	0xf0, 0x0b, 0x66, 0x59, 0x72, 0x93, 0x7a, 0xd3, 0x31, 0x45, 0x9c, 0xfd, 0xf2, 0x60, 0xb0, 0x3e,
	0x1b, 0xf2, 0x00, 0x02, 0x63, 0x59, 0xd5, 0xe0, 0x02, 0x9f, 0x76, 0x84, 0xa4, 0x10, 0x55, 0x8c,
	0x1f, 0x64, 0x2d, 0x6a, 0x56, 0x09, 0x1c, 0x1c, 0xd1, 0xbf, 0x4b, 0x6e, 0x67, 0x6b, 0x84, 0x4e,
	0x06, 0x28, 0x21, 0x76, 0xbb, 0xf6, 0x5a, 0xb5, 0x4d, 0xe2, 0x63, 0xb1, 0x23, 0xe4, 0x21, 0x84,
	0x08, 0x4c, 0x12, 0xa4, 0x83, 0xe9, 0x88, 0xf6, 0xcc, 0xd5, 0x8d, 0xe0, 0xdc, 0xfe, 0x4c, 0x42,
	0x6c, 0xef, 0x59, 0xd6, 0x00, 0xbc, 0xd6, 0xa2, 0x10, 0xb5, 0x95, 0xac, 0x24, 0x8f, 0x20, 0xb0,
	0xee, 0x54, 0xcc, 0x17, 0xcd, 0xa3, 0xee, 0x40, 0xbc, 0x9e, 0x76, 0x0a, 0x79, 0x02, 0xc3, 0x93,
	0xd0, 0x72, 0x27, 0x85, 0xc6, 0xa4, 0xff, 0x75, 0x5d, 0x44, 0xe7, 0xa8, 0xb4, 0xdc, 0xcb, 0xba,
	0x4f, 0xdd, 0xb3, 0xec, 0x03, 0x44, 0xef, 0x84, 0x75, 0xa6, 0x54, 0x18, 0x0c, 0x6c, 0x2c, 0xb3,
	0xad, 0x41, 0xcf, 0x80, 0xf6, 0x8c, 0x4c, 0xc0, 0xe7, 0x5a, 0x14, 0xbd, 0x47, 0xdc, 0x79, 0xfc,
	0x89, 0x4a, 0x51, 0xcd, 0x5e, 0xc0, 0xfd, 0x2f, 0xac, 0x94, 0x05, 0xb3, 0xa2, 0xdb, 0xf8, 0xe3,
	0x32, 0xe8, 0x5d, 0x1d, 0xfc, 0x08, 0xf1, 0xbf, 0x83, 0x57, 0xa2, 0x5c, 0xfe, 0xca, 0xcd, 0x6d,
	0x7f, 0xe5, 0xe9, 0x63, 0x08, 0xbb, 0x67, 0x40, 0xee, 0xc2, 0x68, 0xf9, 0x79, 0xf3, 0xfe, 0xfb,
	0xea, 0xd3, 0xea, 0x4d, 0x7c, 0x87, 0x8c, 0x61, 0x88, 0x74, 0xfd, 0x75, 0x1d, 0x7b, 0xaf, 0x16,
	0xdf, 0x5e, 0xee, 0xa5, 0x3d, 0xb4, 0xdb, 0x19, 0x57, 0x55, 0x5e, 0x30, 0x65, 0x9e, 0x19, 0xcb,
	0xf8, 0x11, 0x61, 0x6e, 0x34, 0xcf, 0xb9, 0xaa, 0xad, 0x56, 0x65, 0x6e, 0x04, 0x6f, 0xb5, 0xb4,
	0xe7, 0xdc, 0x79, 0x2d, 0xdc, 0x67, 0x1b, 0xe2, 0xa3, 0x7c, 0xfe, 0x3b, 0x00, 0x00, 0xff, 0xff,
	0x4d, 0x98, 0xdc, 0xef, 0xa2, 0x02, 0x00, 0x00,
}
